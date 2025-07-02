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

    /** 단일 매수 주문 전체 로직 **/
    @Transactional
    public void placeSingleBuyOrder(StockOrderRequestDTO stockOrderRequestDTO) {
        // 1. 조회 + VO
        User user = findUserWithLock(stockOrderRequestDTO.getUserId());
        Stock stock = findStockByStockCode(stockOrderRequestDTO.getStockCode());
        Money totalPriceVO = calculateTotalPrice(stockOrderRequestDTO);
        Quantity totalQuantityVO = new Quantity(stockOrderRequestDTO.getRequestedQuantity());

        // 2. 도메인 관련 검증 진행
        validateOrderConditions(user, stock, totalPriceVO, totalQuantityVO);
        // 3. 사용자 금액 차감
        deductUserBalance(user, totalPriceVO);
        // 4. Order 및 StockOrder 생성 및 저장
        saveSingleBuyOrder(totalQuantityVO, totalPriceVO, user, stock);
    }

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

    /** 매수 총 금액을 구합니다 **/
    private Money calculateTotalPrice(StockOrderRequestDTO stockOrderRequestDTO) {
        return new Money(stockOrderRequestDTO.getRequestedPrice() * stockOrderRequestDTO.getRequestedQuantity());
    }

    /** Domain Validation **/
    private void validateOrderConditions(User user, Stock stock, Money totalPriceVO, Quantity totalQuantityVO) {
        user.validateSufficientBalance(totalPriceVO);
        stock.validateSufficientQuantity(totalQuantityVO);
    }

    /** User의 balance를 차감합니다 **/
    private void deductUserBalance(User user, Money totalPriceVO) {
        user.withdraw(totalPriceVO);
    }

    /** 단일 매수 주문 저장합니다 **/
    private void saveSingleBuyOrder(Quantity totalQuantityVO, Money totalPriceVO,
                                    User user, Stock stock) {
        // StockOrder / Order 생성
        StockOrder stockOrder = StockOrder.createStockOrder(stock, totalQuantityVO, totalPriceVO, StockOrderStatus.PENDING);
        Order order = Order.createBuyOrder(user, stockOrder, OrderType.BUY, totalPriceVO);

        orderRepository.save(order);
        stockOrderRepository.save(stockOrder, order);
    }
}
