package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 중복 저장을 방지하기 위해서(데이터 정합성)을 위해서 DB 차원의 추가적인 제약 조건을 활용
 *
 */


@Entity
@Getter
@NoArgsConstructor
@Table( uniqueConstraints = {
        @UniqueConstraint(name = "uk_buy_sell_order", columnNames = {"buy_order_id", "sell_order_id"})
})
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private StockOrder buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    private StockOrder sellOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stock stock;

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "trade_price", nullable = false))
    private Money tradePrice; // 거래 단위 당 금액(주당 가격)

    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "trade_quantity", nullable = false))
    private Quantity tradeQuantity; // 거래 수량

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "trade_amount", nullable = false))
    private Money tradeAmount; // tradePrice * tradeQuantity (거래 금액)

    private LocalDateTime tradedAt; // 체결 주문의 체결 시간
    private LocalDateTime createdAt; // DB 저장하는 시간
    private LocalDateTime updatedAt; // 수정 시각

    @Builder
    private Trade(StockOrder buyOrder, StockOrder sellOrder, Stock stock, Money tradePrice,
                 Quantity tradeQuantity, LocalDateTime tradedAt, LocalDateTime updatedAt) {
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.stock = stock;
        this.tradePrice = tradePrice;
        this.tradeQuantity = tradeQuantity;
        this.tradedAt = tradedAt;
        this.updatedAt = tradedAt;
    }

    public static Trade createTrade(StockOrder buyOrder, StockOrder sellOrder, Stock stock, Money tradePrice,
                                   Quantity tradeQuantity, LocalDateTime tradedAt) {
        Trade trade = Trade.builder()
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .stock(stock)
                .tradePrice(tradePrice)
                .tradeQuantity(tradeQuantity)
                .tradedAt(tradedAt)
                .updatedAt(tradedAt)
                .build();

        trade.tradeAmount = new Money(tradePrice.getMoneyValue() * tradeQuantity.getQuantityValue());
        return trade;
    }

    @PrePersist // ??
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.tradedAt = this.createdAt; // 최초 체결 시각
    }

    @PreUpdate // ??
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
