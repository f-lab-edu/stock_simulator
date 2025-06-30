package com.portfolio2025.first.domain.stock;

import com.portfolio2025.first.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;

@Entity
@Table(name = "stock_orders")
public class StockOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 주문 연관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 🔗 종목 연관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "executed_price")
    private Long executedPrice;

    @Column(name = "executed_quantity")
    private Long executedQuantity;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "order_price", nullable = false)
    private Long orderPrice;

    @Column(name = "order_quantity", nullable = false)
    private Long orderQuantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_order_status", nullable = false)
    private StockOrderStatus stockOrderStatus;

    @Column(name = "stock_price")
    private Long stockPrice;

    @Column(name = "stock_name")
    private String stockName;

    @Builder
    private StockOrder(Long id, Order order, Stock stock, Long executedPrice, Long executedQuantity,
                      LocalDateTime executedAt, Long orderPrice, Long orderQuantity, LocalDateTime createdAt,
                      LocalDateTime updatedAt, StockOrderStatus stockOrderStatus, Long stockPrice,
                      String stockName) {
        this.id = id;
        this.order = order;
        this.stock = stock;
        this.executedPrice = executedPrice;
        this.executedQuantity = executedQuantity;
        this.executedAt = executedAt;
        this.orderPrice = orderPrice;
        this.orderQuantity = orderQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stockOrderStatus = stockOrderStatus;
        this.stockPrice = stockPrice;
        this.stockName = stockName;
    }

    public static StockOrder create(Order order, Stock stock, Long executedQuantity, Long executedPrice) {
        return StockOrder.builder()
                .order(order)
                .stock(stock)
                .orderPrice(executedPrice)
                .orderQuantity(executedQuantity)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .stockOrderStatus(StockOrderStatus.PENDING)
                .stockPrice(stock.getStockPrice())
                .stockName(stock.getStockName())
                .build();
    }
}

