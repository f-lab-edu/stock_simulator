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
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매수, 매도 주문 생성을 담당하는 StockOrderService
 *
 * [07.30]
 * (수정) portfolio 조회 시 비관적 락 적용
 * (수정) flush() 메서드 제외 -> flush() 해야 하는 상황은 어떤게 있을지도 고민해보기
 *
 * [고민]
 * 1. DTO 생성 중복 로직이 많이 발생하는 상황
 * 2. DB 락 배정한 상황 - DB 정합성이 더 중요하게 고려되는 상황
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;

    private final RedissonClient redissonClient;
    private final StockOrderRepository stockOrderRepository;
    private final RedisStockOrderService redisStockOrderService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매수 또는 매도 단일 주문 공통 로직 (분산 락 같이 포함시켜야 함)
     */
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

    /**
     * 주문 수정 로직
     */
    public void modifyStockOrder(ModifyStockOrderRequestDTO dto) {
        // lock:stockOrder - 주문 관련 락 Key
        String lockKey = "lock:stockOrder:" + dto.getStockOrderId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean available = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!available) {
                throw new IllegalStateException("다른 사용자가 해당 주문을 수정 중입니다.");
            }

            // 트랜잭션 추가해야 하는데 락과 분리되서 진행되어야 함 - 아래 로직은 따로 빼서 @Transactional 붙이고 호출하는 방식으로
            // 2. StockOrder 조회 + 상태 확인
            StockOrder stockOrder = stockOrderRepository.findByIdWithAllRelations(dto.getStockOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다."));

            // 사용자 권한 확인
            Long orderOwnerId = stockOrder.getOrder().getUser().getId();
            if (!orderOwnerId.equals(dto.getUserId())) {
                throw new AccessDeniedException("해당 주문에 대한 수정 권한이 없습니다.");
            }

            // 주문 상태 확인
            if (stockOrder.getStockOrderStatus() != StockOrderStatus.PENDING) {
                throw new IllegalStateException("체결 중이거나 완료된 주문은 수정할 수 없습니다.");
            }

            // 3. 기존 주문 취소 처리
            // 주문 상태 변경
            stockOrder.updateStatus(StockOrderStatus.CANCELLED);

            // Redis 삭제 (BUY / SELL 구분 필요)
            StockOrderRedisDTO dtoForRedis = StockOrderRedisDTO.from(stockOrder);

            if (stockOrder.getOrder().getOrderType() == OrderType.BUY) {
                redisStockOrderService.removeBuyOrder(dtoForRedis);
            } else {
                // 매도 주문일 경우 매도용 삭제 메서드 추가 필요
                redisStockOrderService.removeSellOrder(dtoForRedis);
            }

            // 4. 새 주문 생성 및 저장
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

            // 5. Kafka 발행, Redis 업데이트 (afterCommit) - 신규 주문 생성과 동일하게 진행
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

        } catch (InterruptedException | AccessDeniedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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

