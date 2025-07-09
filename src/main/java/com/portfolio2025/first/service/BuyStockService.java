package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.StockOrderFactory;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.stock.StockOrderStatus;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuyStockService {
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final StockOrderFactory stockOrderFactory;

    /** 단일 매수 주문 전체 로직 **/
    @Transactional
    public void placeSingleBuyOrder(StockOrderRequestDTO stockOrderRequestDTO) {
        // 1. 조회 + VO
        User user = findUserWithLock(stockOrderRequestDTO.getUserId());
        Stock stock = findStockByStockCode(stockOrderRequestDTO.getStockCode());
        Money totalPriceVO = calculateTotalPrice(stockOrderRequestDTO);
        Quantity totalQuantityVO = new Quantity(stockOrderRequestDTO.getRequestedQuantity());
        // 2. 도메인 관련 검증 진행
        stock.reserve(totalQuantityVO);
//        user.buy(totalPriceVO);
        // 3. Order 및 StockOrder 생성 및 저장 -> user, stock 만 인자로 전달하는 방식과 비교해서 생각하기
        saveSingleBuyOrder(totalQuantityVO, totalPriceVO, user, stock);
    }

    // validate 호출부에서 진행하는 걸로
    @Transactional
    public void placeBulkBuyOrder(List<StockOrderRequestDTO> stockOrderRequestDTOList) {
        // List<> 형식의 검증은 DTO 내에서 한번에 처리 불가해서 따로 한번 더 처리함
        validateDTOs(stockOrderRequestDTOList);

        // 1. User ID 통일성 검증 (다른 유저 ID 섞이면 예외)
        Long firstUserId = stockOrderRequestDTOList.getFirst().getUserId();
        validateIfSameUser(stockOrderRequestDTOList, firstUserId);

        // 2. 유저 조회 (Lock)
        User user = findUserWithLock(firstUserId);

        List<StockOrder> stockOrders = stockOrderRequestDTOList.stream()
                .map(stockOrderFactory::createStockOrder)
                .toList();

        Money totalPrice = calculateTotalPrice(stockOrders);
//        user.buy(totalPrice);
        orderRepository.save(Order.createBulkBuyOrder(user, stockOrders, OrderType.BUY, totalPrice));
    }

    private void validateDTOs(List<StockOrderRequestDTO> stockOrderRequestDTOList) {
        if (stockOrderRequestDTOList == null || stockOrderRequestDTOList.isEmpty()) {
            throw new IllegalArgumentException("주문 요청 리스트가 비어 있습니다.");
        }
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
//        user.validateSufficientBalance(totalPriceVO);
        stock.validateSufficientQuantity(totalQuantityVO);
    }

    /** User의 balance를 차감합니다 **/
    private void deductUserBalance(User user, Money totalPriceVO) {
//        user.withdraw(totalPriceVO);
    }

    /** 단일 매수 주문 저장합니다 **/
    private void saveSingleBuyOrder(Quantity totalQuantityVO, Money totalPriceVO,
                                    User user, Stock stock) {
        // StockOrder / Order 생성
        StockOrder stockOrder = StockOrder.createStockOrder(stock, totalQuantityVO, totalPriceVO, StockOrderStatus.PENDING);
        Order order = Order.createSingleBuyOrder(user, stockOrder, OrderType.BUY, totalPriceVO);

        orderRepository.save(order);
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
