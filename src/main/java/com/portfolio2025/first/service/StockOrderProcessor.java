package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;

public interface StockOrderProcessor {
    // 유효성 확인 및 예약
    void reserve(Portfolio portfolio, Stock stock, Quantity quantity, Money totalPrice);
    // 주문 생성
    Order createOrder(Portfolio portfolio, Stock stock, Quantity quantity, Money unitPrice);
    // Kafka 이벤트 발행하기
    void publishEvent(Order order, Portfolio portfolio, Stock stock, Quantity quantity, Money price);
    // 매수 or 매도인지?
    OrderType getOrderType();
}
