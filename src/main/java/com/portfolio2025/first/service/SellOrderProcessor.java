package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SellOrderProcessor implements StockOrderProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final PortfolioStockRepository portfolioStockRepository;

    @Override
    public void reserve(Portfolio portfolio, Stock stock, Quantity quantity, Money totalPrice) {
        PortfolioStock portfolioStock = portfolioStockRepository
                .findByPortfolioAndStockWithLock(portfolio, stock)
                .orElseThrow(() -> new IllegalStateException("보유한 주식이 없습니다."));
        portfolioStock.reserve(quantity);
    }

    @Override
    public Order createOrder(Portfolio portfolio, Stock stock, Quantity quantity, Money unitPrice) {
        StockOrder stockOrder = StockOrder.createStockOrder(stock, quantity, unitPrice, portfolio);
        Money totalPrice = unitPrice.multiply(quantity);
        return Order.createSingleOrder(portfolio, stockOrder, OrderType.SELL, totalPrice);
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
                OrderType.SELL.name()
        );

        eventPublisher.publishEvent(event);
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.SELL;
    }
}

