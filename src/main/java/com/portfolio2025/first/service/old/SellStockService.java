package com.portfolio2025.first.service.old;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 매도 관련 로직
 * placeSingleBuyOrder(StockOrderRequestDTO):
 * 1. User, Portfolio, Stock 조회
 * 2. 수량·금액 계산 및 유효성 검사
 * 3. 보유 주식 유효성 검증 + 예약 처리
 * 4. 주문 및 주문 상세 생성 및 저장
 * 5. Kafka 이벤트 구성
 * 6. 커밋 이후 Kafka 메시지 발행 (registerSynchronization)
 * **/

@Service
@RequiredArgsConstructor
public class SellStockService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;
    private final PortfolioStockRepository portfolioStockRepository;

    @Transactional
    public void placeSingleSellOrder(StockOrderRequestDTO stockOrderRequestDTO) {
        // 1. 조회
        User user = findUserWithLock(stockOrderRequestDTO.getUserId());
        Portfolio portfolio = user.getDefaultPortfolio()
                .orElseThrow(() -> new IllegalArgumentException("투자용 포트폴리오가 존재하지 않습니다."));
        Stock stock = findStockByStockCode(stockOrderRequestDTO.getStockCode());

        Quantity quantity = new Quantity(stockOrderRequestDTO.getRequestedQuantity());
        Money requestedPrice = new Money(stockOrderRequestDTO.getRequestedPrice());
        Money totalPrice = requestedPrice.multiply(quantity);

        // 2. 보유 주식 수량 검증 (예약 방식)
        PortfolioStock portfolioStock = portfolioStockRepository
                .findByPortfolioAndStock(portfolio, stock)
                .orElseThrow(() -> new IllegalStateException("보유한 주식이 없습니다."));

        reserveWithValidation(quantity, portfolioStock);

        // 3. 주문 객체 생성
        StockOrder stockOrder = StockOrder.createStockOrder(stock, quantity, requestedPrice, portfolio);
        Order order = Order.createSingleBuyOrder(portfolio, stockOrder, OrderType.SELL, totalPrice);
        orderRepository.save(order);

        // +@ DB 반영해주기
        orderRepository.flush();

        // 4. Kafka 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                portfolio.getUser().getId(),
                portfolio.getId(),
                stock.getStockCode(),
                quantity.getQuantityValue(),
                requestedPrice.getMoneyValue(),
                OrderType.SELL.name()
        );


        // 5. 커밋 이후에 발행을 등록함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                System.out.println("registerSynchronization on selling stock");
                kafkaProducerService.publishOrderCreated(event);
            }
        });
    }

    private void reserveWithValidation(Quantity quantity, PortfolioStock portfolioStock) {
        portfolioStock.reserve(quantity); // 실제 차감은 하지 않음, 예약 처리
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
