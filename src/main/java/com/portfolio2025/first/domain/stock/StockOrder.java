package com.portfolio2025.first.domain.stock;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
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

/**
 * 매수 Or 매도 주문 StockOrder
 * [07.26]
 * (수정) updateQuantity -> 메서드 네이밍 수정 + 내부 로직 분리 (calculateWeightedAverage, updateStockOrderStatus)
 *
 * [고민]
 * 1. averageExecutedPrice 구체적 기획 생각해보기
 */
@Entity
@Table(name = "stock_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상위 주문 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 종목 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // 포트폴리오 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

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

    // 남은 수량, 미체결 수량
    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "remained_quantity", nullable = false))
    private Quantity remainedQuantity;

    // 평균 체결 단가 (체결 없으면 null 허용) -- deprecated
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
    private StockOrder(Stock stock, Quantity requestedQuantity, Money requestedPrice,
                       StockOrderStatus stockOrderStatus, Portfolio portfolio) {
        this.stock = stock;
        this.requestedQuantity = requestedQuantity;
        this.requestedPrice = requestedPrice;
        this.executedQuantity = new Quantity(0L);  // 최초 체결 수량은 0
        this.remainedQuantity = requestedQuantity; // 최초 미체결 수량은 요청 수량과 같음
        this.portfolio = portfolio;
        this.stockOrderStatus = stockOrderStatus;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static StockOrder createStockOrder(Stock stock, Quantity requestedQuantity, Money requestedPrice,
                                              Portfolio portfolio) {
        return StockOrder.builder()
                .stock(stock)
                .portfolio(portfolio)
                .requestedQuantity(requestedQuantity)
                .requestedPrice(requestedPrice)
                .stockOrderStatus(StockOrderStatus.PENDING)
                .build();
    }

    /** 양방향 연관관계 메서드 */
    public void setOrder(Order order) {
        this.order = order;
    }

    // 상태 변경
    public void updateStatus(StockOrderStatus newStatus) {
        this.stockOrderStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    // 체결 반영
    public void applyExecution(Quantity executingQuantity, Money executingPrice) {
        Quantity newExecuted = this.executedQuantity.plus(executingQuantity);
        Money newAveragePrice = calculateWeightedAverage(executingQuantity, executingPrice);

        this.executedQuantity = newExecuted;
        this.remainedQuantity = this.requestedQuantity.minus(newExecuted);
        this.averageExecutedPrice = newAveragePrice;
        this.updatedAt = LocalDateTime.now();

        updateStockOrderStatus();
    }

    // 평균 체결 단가 계산 로직
    private Money calculateWeightedAverage(Quantity executingQuantity, Money executingPrice) {
        long prevExecutedQty = this.executedQuantity.getQuantityValue();
        long prevTotal = (this.averageExecutedPrice != null)
                ? this.averageExecutedPrice.getMoneyValue() * prevExecutedQty
                : 0L;

        long newTotal = executingQuantity.getQuantityValue() * executingPrice.getMoneyValue();
        long totalQty = prevExecutedQty + executingQuantity.getQuantityValue();

        return new Money((prevTotal + newTotal) / totalQty);
    }

    // 주문 상태 갱신 로직
    private void updateStockOrderStatus() {
        this.stockOrderStatus = this.remainedQuantity.isZero()
                ? StockOrderStatus.FILLED
                : StockOrderStatus.PARTIALLY_FILLED;
    }
}

