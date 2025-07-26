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

@Component
@RequiredArgsConstructor
public class BuyOrderProcessor implements StockOrderProcessor {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void reserve(Portfolio portfolio, Stock stock, Quantity quantity, Money totalPrice) {
//        stock.reserve(quantity); // 검증만 진행하고 실제 차감은 하지 않음 -> 변경 (매수자 입장에서 주식 수량은 따로 검증하지 않아도 됨)
        portfolio.reserveCash(totalPrice); // 검증 + 실제 차감까지 진행함
    }

    @Override
    public Order createOrder(Portfolio portfolio, Stock stock, Quantity quantity, Money unitPrice) {
        StockOrder stockOrder = StockOrder.createStockOrder(stock, quantity, unitPrice, portfolio);
        Money totalPrice = unitPrice.multiply(quantity);
        return Order.createSingleBuyOrder(portfolio, stockOrder, OrderType.BUY, totalPrice);
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

        eventPublisher.publishEvent(event);
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.BUY;
    }
}
