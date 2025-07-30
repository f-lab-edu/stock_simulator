package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 매수 주문을 담당하는 BuyOrderProcessor - implements StockOrderProcessor
 *
 * [07.30]
 * (수정) 매수 주문 생성 시 주식의 수량은 따로 검증하지 않아도 되기 때문에 제외함(차라리 제한 로직을 두는게 더 나을 듯 - 최대 100개만)
 *
 * [고민]
 *
 *
 */
@Component
@RequiredArgsConstructor
public class BuyOrderProcessor implements StockOrderProcessor {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void reserve(Portfolio portfolio, Stock stock, Quantity quantity, Money totalPrice) {
        // 매수 시 제한을 걸어야 하는 경우 이 곳에서 가능함
        portfolio.reserveAndDeductCash(totalPrice); // 검증 + 실제 차감까지 진행함 - 네이밍 수정
    }

    @Override
    public Order createOrder(Portfolio portfolio, Stock stock, Quantity quantity, Money unitPrice) {
        StockOrder stockOrder = StockOrder.createStockOrder(stock, quantity, unitPrice, portfolio);
        Money totalPrice = unitPrice.multiply(quantity);
        return Order.createSingleOrder(portfolio, stockOrder, OrderType.BUY, totalPrice);
    }

    @Override
    public void publishEvent(Order order, Portfolio portfolio, Stock stock, Quantity quantity, Money price) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                portfolio.getUser().getId(),
                portfolio.getId(),
                stock.getStockCode(),
                quantity.getQuantityValue(),
                price.getMoneyValue(),
                OrderType.BUY.name()
        );
        // OrderRedisSyncListener에서 해당 이벤트 감지
        eventPublisher.publishEvent(event);
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.BUY;
    }
}
