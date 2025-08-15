package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.stock.StockOrderStatus;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.ModifyStockOrderRequestDTO;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.lock.DistributedLock;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매수, 매도 주문 생성을 담당하는 StockOrderService
 *
 * [07.30]
 * (추가) DistributedLock Annotation -> 분산 락 획득하고 생성, 수정, 삭제 진행 예정
 * (수정) modify.. -> Annotation 활용 기존 로직 리팩토링 진행하기
 *
 * [고민]
 * 1. 사용자, 종목 정보를 포함한 키로 분산 락을 적용해야 하지 않을까?? (동일 사용자의 동일 종목 주문이 발생하는 경우 대비하고자 함)
 * 2. DB 락 - Portfolio, User 배정한 상황
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;

    private final StockOrderRepository stockOrderRepository;
    private final RedisStockOrderService redisStockOrderService;
    private final ApplicationEventPublisher eventPublisher;

    @DistributedLock(
            prefix = "order",                     // 락 키 접두사
            key = "#dto.userId + ':' + #dto.stockCode", // EL 표현식으로 키 구성
            waitTime = 3L,
            leaseTime = 10L,
            timeUnit = TimeUnit.SECONDS
    ) // order:1L:00.. 형태로 Lock을 획득할 수 있다
    @Transactional
    public void placeSingleOrder(StockOrderRequestDTO dto, StockOrderProcessor processor) {
        // 1. 조회
        User user = findUserWithLock(dto.getUserId());
        Portfolio portfolio = getDefaultPortfolioWithLock(user.getId());
        Stock stock = findStockByStockCode(dto.getStockCode());

        // 2. 계산
        Quantity quantity = new Quantity(dto.getRequestedQuantity());
        Money unitPrice = new Money(dto.getRequestedPrice());
        Money totalPrice = unitPrice.multiply(quantity);

        // 3. 검증 및 예약 처리 (전략에 따라)
        processor.reserve(portfolio, stock, quantity, totalPrice);

        // 4. 주문 객체 생성 및 저장
        Order order = processor.createOrder(portfolio, stock, quantity, unitPrice);
        orderRepository.save(order);

        // 5. 이벤트 발행
        processor.publishEvent(order, portfolio, stock, quantity, unitPrice);
    }

    // 주문 수정 로직
    @DistributedLock(
            prefix = "order",
            key = "#dto.userId + ':' + #dto.stockOrderId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS
    )
    public void modifyStockOrder(ModifyStockOrderRequestDTO dto) throws AccessDeniedException {
        // 1. StockOrder 조회
        StockOrder stockOrder = stockOrderRepository.findByIdWithAllRelations(dto.getStockOrderId())
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다."));

        // 2. 사용자 권한 확인
        Long orderOwnerId = stockOrder.getOrder().getUser().getId();
        if (!orderOwnerId.equals(dto.getUserId())) {
            throw new AccessDeniedException("해당 주문에 대한 수정 권한이 없습니다.");
        }

        // 3. 주문 상태 확인
        if (stockOrder.getStockOrderStatus() != StockOrderStatus.PENDING) {
            throw new IllegalStateException("체결 중이거나 완료된 주문은 수정할 수 없습니다.");
        }

        // 4. 기존 주문 취소 처리
        stockOrder.updateStatus(StockOrderStatus.CANCELLED);
        StockOrderRedisDTO dtoForRedis = StockOrderRedisDTO.from(stockOrder);

        if (stockOrder.getOrder().getOrderType() == OrderType.BUY) {
            redisStockOrderService.removeBuyOrder(dtoForRedis);
        } else {
            redisStockOrderService.removeSellOrder(dtoForRedis);
        }

        // 5. 새 주문 생성
        Quantity newQuantity = new Quantity(dto.getRequestedQuantity());
        Money newPrice = new Money(dto.getRequestedPrice());

        StockOrder newStockOrder = StockOrder.createStockOrder(
                stockOrder.getStock(), newQuantity, newPrice, stockOrder.getPortfolio()
        );

        OrderType orderType = stockOrder.getOrder().getOrderType();
        Money totalPrice = newPrice.multiply(newQuantity);
        Order newOrder = Order.createSingleOrder(
                stockOrder.getPortfolio(), newStockOrder, orderType, totalPrice
        );

        orderRepository.save(newOrder);

        // 6. Kafka 발행
        StockOrder updatedStockOrder = newOrder.getStockOrders().getFirst();

        OrderCreatedEvent event = new OrderCreatedEvent(
                newOrder.getId(),
                updatedStockOrder.getPortfolio().getUser().getId(),
                updatedStockOrder.getPortfolio().getId(),
                updatedStockOrder.getStock().getStockCode(),
                updatedStockOrder.getRequestedQuantity().getQuantityValue(),
                updatedStockOrder.getRequestedPrice().getMoneyValue(),
                newOrder.getOrderType().name()
        );

        eventPublisher.publishEvent(event);
    }

    private Portfolio getDefaultPortfolioWithLock(Long userId) {
        return portfolioRepository.findByUserIdAndPortfolioTypeWithLock(userId, PortfolioType.STOCK)
                .orElseThrow(() -> new IllegalArgumentException("투자용 포트폴리오가 존재하지 않습니다."));
    }

    private User findUserWithLock(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Stock findStockByStockCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
    }
}

