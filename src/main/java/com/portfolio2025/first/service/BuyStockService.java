package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * 매수 관련 로직
 * placeSingleBuyOrder(StockOrderRequestDTO):
 * 1. User, Portfolio, Stock 조회
 * 2. 수량·금액 계산 및 유효성 검사
 * 3. 도메인 상태 변경 (reserveCash, reserveQuantity)
 * 4. 주문 및 주문 상세 생성 및 저장
 * 5. Kafka 이벤트 구성
 * 6. 커밋 이후 Kafka 메시지 발행 (registerSynchronization)
 * **/

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyStockService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    /** 단일 매수 주문 전체 로직 **/
    @Transactional
    public void placeSingleBuyOrder(StockOrderRequestDTO dto) {
        // 1. 조회
        User user = findUserWithLock(dto.getUserId());
        Portfolio portfolio = user.getDefaultPortfolio()
                .orElseThrow(() -> new IllegalArgumentException("투자용 포트폴리오가 존재하지 않습니다."));
        Stock stock = findStockByStockCode(dto.getStockCode());

        // 2. 기본 계산 및 검증
        Money requestedPrice = new Money(dto.getRequestedPrice());
        Money totalPrice = calculateTotalPrice(dto);
        Quantity totalQuantity = new Quantity(dto.getRequestedQuantity());

        // 유통량과 비교(Stock과 비교 진행함)
        stock.reserve(totalQuantity);
        // availableCash, reservedCash update(사용 가능한 금액은 차감, 예약 금액은 상승)
        portfolio.reserveCash(totalPrice);

        // 3. Order 및 StockOrder 생성 및 저장
        Order savedOrder = createAndSaveSingleOrder(totalQuantity,
                new Money(dto.getRequestedPrice()), portfolio, stock);

        // +@ 외부 연동 전 flush()
        orderRepository.flush();

        // 4. KafkaProducer -> 이벤트 발행하는 시점
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                portfolio.getUser().getId(),
                portfolio.getId(),
                stock.getStockCode(),
                totalQuantity.getQuantityValue(),
                requestedPrice.getMoneyValue(),
                OrderType.BUY.name()
        );

        // 5. 커밋 이후에 발행을 등록함 - registerSynchronization 적용함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                System.out.println("registerSynchronization on buying");
                kafkaProducerService.publishOrderCreated(event);
            }
        });
    }

    /** 단일 매수 주문 전체 로직 **/
    @Transactional
    public void placeBulkBuyOrder(List<StockOrderRequestDTO> stockOrderRequestDTOList) {
        // List<> 형식의 검증은 DTO 내에서 한번에 처리 불가해서 따로 한번 더 처리함 - Controller 에서 받을 떄 진행하기
        /******* 수정해야 하는 메서드 ******/
        validateDTOs(stockOrderRequestDTOList);

        // 1. User ID 통일성 검증 (다른 유저 ID 섞이면 예외)
        Long firstUserId = stockOrderRequestDTOList.getFirst().getUserId();
        validateIfSameUser(stockOrderRequestDTOList, firstUserId);

        // 2. 유저 조회 (Lock)
        User user = findUserWithLock(firstUserId);
        Portfolio portfolio = user.getDefaultPortfolio()
                .orElseThrow(() -> new IllegalArgumentException("투자용 포트폴리오가 존재하지 않습니다."));

        // 💡 stockOrder 생성
        List<StockOrder> stockOrders = stockOrderRequestDTOList.stream()
                .map(dto -> {
                    Stock stock = findStockByStockCode(dto.getStockCode());

                    Quantity quantity = new Quantity(dto.getRequestedQuantity());
                    Money orderPrice = new Money(dto.getRequestedPrice() * dto.getRequestedQuantity());

                    // 주문 수량 예약
                    stock.reserve(quantity);
                    return StockOrder.createStockOrder(stock, quantity, orderPrice, portfolio);
                })
                .toList();

        Money totalPrice = calculateTotalPrice(stockOrders);
        Quantity totalQuantity = calculateTotalQuantity(stockOrders);

        portfolio.buy(totalPrice, totalQuantity);
        createAndSaveBulkBuyOrder(portfolio, stockOrders, OrderType.BUY, totalPrice);
    }

    private void createAndSaveBulkBuyOrder(Portfolio portfolio, List<StockOrder> stockOrders,
                                           OrderType orderType, Money totalPrice) {
        orderRepository.save(Order.createBulkBuyOrder(portfolio, stockOrders, orderType, totalPrice));
    }

    // Method 구조 생각해보기
    private Quantity calculateTotalQuantity(List<StockOrder> stockOrders) {
        return stockOrders.stream()
                .map(StockOrder::getRequestedQuantity)
                .reduce(new Quantity(0L), Quantity::plus);
    }

    // Controller 단에서 진행하는 걸로 수정하기
    private void validateDTOs(List<StockOrderRequestDTO> stockOrderRequestDTOList) {
        if (stockOrderRequestDTOList == null || stockOrderRequestDTOList.isEmpty()) {
            throw new IllegalArgumentException("주문 요청 리스트가 비어 있습니다.");
        }
    }

    /** 락 + userId로 조회를 진행합니다 **/
    private User findUserWithLock(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /** 락 없이 stockId로 조회를 진행합니다 **/
    private Stock findStockById(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
        return stock;
    }

    /** 락 없이 stockCode로 조회를 진행합니다 **/
    private Stock findStockByStockCode(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
        return stock;
    }

    /** 매수 총 금액을 구합니다 **/
    private Money calculateTotalPrice(StockOrderRequestDTO stockOrderRequestDTO) {
        return new Money(stockOrderRequestDTO.getRequestedPrice() * stockOrderRequestDTO.getRequestedQuantity());
    }


    /** 단일 매수 주문 저장합니다 **/
    private Order createAndSaveSingleOrder(Quantity totalQuantity, Money requestedPrice,
                                          Portfolio portfolio, Stock stock) {
        // StockOrder / Order 생성
        StockOrder stockOrder = StockOrder.createStockOrder(stock, totalQuantity, requestedPrice, portfolio);
        // totalPrice 구해야 함
        Money totalPrice = requestedPrice.multiply(totalQuantity);
        Order order = Order.createSingleBuyOrder(portfolio, stockOrder, OrderType.BUY, totalPrice);

        return orderRepository.save(order);
    }

    private void validateIfSameUser(List<StockOrderRequestDTO> stockOrderRequestDTOList, Long firstUserId) {
        boolean allSameUser = stockOrderRequestDTOList.stream()
                .allMatch(dto -> dto.getUserId().equals(firstUserId));

        if (!allSameUser) {
            throw new IllegalArgumentException("모든 주문 요청의 userId가 동일해야 합니다.");
        }
    }

    private Money calculateTotalPrice(List<StockOrder> stockOrders) {
        return stockOrders.stream()
                .map(StockOrder::getRequestedPrice)
                .reduce(new Money(0L), Money::plus);
    }
}
