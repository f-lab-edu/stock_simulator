package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    /**
     * 매수 또는 매도 단일 주문 공통 로직
     */
    @Transactional
    public void placeSingleOrder(StockOrderRequestDTO dto, StockOrderProcessor processor) {
        // 1. 조회
        User user = findUserWithLock(dto.getUserId());
        // Lock 필요함 - PESSIMISTIC / REDIS lock (추후 리팩토링 진행 예정)
        Portfolio portfolio = getDefaultPortfolio(user);
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
        orderRepository.flush();

        // 5. 이벤트 발행
        processor.publishEvent(order, portfolio, stock, quantity, unitPrice);
    }

    private Portfolio getDefaultPortfolio(User user) {
        return user.getDefaultPortfolio()
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

