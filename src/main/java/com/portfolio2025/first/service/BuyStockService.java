package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrderStatus;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuyStockService
{
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final StockOrderRepository stockOrderRepository;

    /** 단일 매수 주문 넣기 **/
    @Transactional
    public void placeSingleBuyOrder(StockOrderRequestDTO stockOrderRequestDTO) {
        // 1. 조회
        User user = findUserWithLock(stockOrderRequestDTO.getUserId());
        Stock stock = findStockByStockCode(stockOrderRequestDTO.getStockCode());
        Long requestedQuantity = stockOrderRequestDTO.getRequestedQuantity();
        Long requestedPrice = stockOrderRequestDTO.getRequestedPrice();

        // 2. 도메인 관련 검증 진행
        Money totalPriceVO = new Money(requestedQuantity * requestedPrice);
        Quantity totalQuantityVO = new Quantity(requestedQuantity);
        user.validateSufficientBalance(totalPriceVO);
        stock.validateSufficientQuantity(totalQuantityVO);

        // 3. 사용자 금액 차감
        user.withdraw(totalPriceVO);

        // 4. Order 및 StockOrder 생성 및 저장
        saveSingleBuyOrder(totalQuantityVO, totalPriceVO, user, stock);
    }

    // Bulk API도 지원해야 함



    /** 락 + userId로 조회를 진행합니다 **/
    private User findUserWithLock(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user;
    }

    /** 락 없이 stockId로 조회를 진행합니다 **/
    private Stock findStock(Long stockId) {
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

    private void saveSingleBuyOrder(Quantity totalQuantityVO, Money totalPriceVO,
                                    User user, Stock stock) {
        // StockOrder / Order 생성
        StockOrder stockOrder = StockOrder.createStockOrder(stock, totalQuantityVO, totalPriceVO, StockOrderStatus.PENDING);
        Order order = Order.createBuyOrder(user, stockOrder, OrderType.BUY, totalPriceVO);

        orderRepository.save(order);
        stockOrderRepository.save(stockOrder, order);
    }
}
