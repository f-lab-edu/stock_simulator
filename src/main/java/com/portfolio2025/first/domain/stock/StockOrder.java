package com.portfolio2025.first.domain.stock;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 상위 주문 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 🔗 종목 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // 최초 주문 수량
    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "requested_quantity", nullable = false))
    private Quantity requestedQuantity;

    // 최초 주문 희망 가격
    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "requested_price", nullable = false))
    private Money requestedPrice;

    // 현재까지 체결된 수량
    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "executed_quantity", nullable = false))
    private Quantity executedQuantity;

    // 남음 수량, 미체결 수량
    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "remained_quantity", nullable = false))
    private Quantity remainedQuantity;

    // 평균 체결 단가 (체결 없으면 null 허용)
    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "average_executed_price"))
    private Money averageExecutedPrice;

    // 현재 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_order_status", nullable = false)
    private StockOrderStatus stockOrderStatus;  // PENDING / PARTIALLY_FILLED / FILLED / CANCELLED 등

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private StockOrder(Stock stock, Quantity requestedQuantity, Money requestedPrice, StockOrderStatus stockOrderStatus) {
        this.stock = stock;
        this.requestedQuantity = requestedQuantity;
        this.requestedPrice = requestedPrice;
        this.executedQuantity = new Quantity(0L);  // 최초 체결 수량은 0
        this.remainedQuantity = new Quantity(0L);
        this.stockOrderStatus = stockOrderStatus;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static StockOrder createStockOrder(Stock stock, Quantity requestedQuantity, Money requestedPrice,
                                              StockOrderStatus stockOrderStatus) {
        return StockOrder.builder()
                .stock(stock)
                .requestedQuantity(requestedQuantity)
                .requestedPrice(requestedPrice)
                .stockOrderStatus(StockOrderStatus.PENDING)
                .build();
    }

    /** 양방향 연관관계 메서드 */
    public void setOrder(Order order) {
        this.order = order;
    }

    /** 체결 업데이트 */
    public void updateExecution(Quantity newExecutedQuantity, Money newAveragePrice, LocalDateTime executionTime) {
        this.executedQuantity = newExecutedQuantity;
        this.averageExecutedPrice = newAveragePrice;
        this.executedAt = executionTime;
        this.updatedAt = LocalDateTime.now();
    }

    /** 상태 변경 시 */
    public void updateStatus(StockOrderStatus newStatus) {
        this.stockOrderStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}

