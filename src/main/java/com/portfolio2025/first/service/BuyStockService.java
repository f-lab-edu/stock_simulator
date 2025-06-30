package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Money;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuyStockService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioStockRepository portfolioStockRepository;
    private final OrderRepository orderRepository;
    private final StockOrderRepository stockOrderRepository;

    /**
     * @param userId - 유저 조회
     * @param stockId - 종목 조회
     * @param desiredQuantity - 희망 수량
     * @param desiredPrice - 희망 가격
     *
     *
     */

    // 메수 주문 넣기
    @Transactional
    public void placeBuyOrder(Long userId, Long accountId, Long stockId, Long desiredQuantity, Long desiredPrice) {
        // 1. 유저 조회 w/ Lock
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // 2. 주식 조회 w/o lock
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        // 3. 입력 수량 유효성 검사
        validateQuantity(desiredQuantity);
        validatePrice(desiredPrice);
        // 4. 총 매수 금액 계산
        Long totalPrice = desiredQuantity * desiredPrice;
        // 5. 사용자 잔액 체크
        if (user.isBalanceInsufficient(new Money(totalPrice))) {
            throw new IllegalArgumentException("Insufficient account balance");
        }
        // 6. 주식 시장 재고 체크 (함수화)
        if (stock.hasLowerQuantityThan(desiredQuantity)) {
            throw new IllegalArgumentException("Not enough stock quantity in market");
        }

        // 7. 사용자 금액 차감
        user.withdraw(new Money(totalPrice));

        // 8. Order 및 StockOrder 생성 및 저장
        Order order = Order.createBuyOrder(user, totalPrice);
        orderRepository.save(order);

        StockOrder stockOrder = StockOrder.create(order, stock, desiredQuantity, desiredPrice);
        stockOrderRepository.save(stockOrder);
    }

    private void validatePrice(Long price) {
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("Price must be a positive number");
        }
    }

    private void validateQuantity(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number");
        }
    }
}
