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
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
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
    private Money tradePrice;

    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "trade_quantity", nullable = false))
    private Quantity tradeQuantity;

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "trade_amount", nullable = false))
    private Money tradeAmount;

    private LocalDateTime tradedAt; // 체결 주문의 체결 시간
    private LocalDateTime createdAt; // DB 저장하는 시간
    private LocalDateTime updatedAt; // 백업하는 용도?

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
