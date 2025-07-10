package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.stock.Stock;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 구체적인 체결 내역을 관리합니다 **/

@Entity
@Table(name = "portfolio_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Portfolio 연관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    // 🔗 Stock 연관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Embedded
    @AttributeOverride(name = "quantity_value", column = @Column(name = "portfolio_quantity", nullable = false))
    private Quantity portfolioQuantity;

    @Embedded
    @AttributeOverride(name = "price_value", column = @Column(name = "portfolio_average_price", nullable = false))
    private Money portfolioAveragePrice;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Builder
    private PortfolioStock(Portfolio portfolio, Stock stock, Quantity portfolioQuantity,
                          Money portfolioAveragePrice, LocalDateTime lastUpdatedAt) {
        this.portfolio = portfolio;
        this.stock = stock;
        this.portfolioQuantity = portfolioQuantity;
        this.portfolioAveragePrice = portfolioAveragePrice;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    // static 생성자
    public static PortfolioStock createPortfolioStock(Portfolio portfolio, Stock stock, Quantity portfolioQuantity,
                                        Money portfolioAveragePrice) {
        return PortfolioStock.builder()
                .portfolio(portfolio)
                .stock(stock)
                .portfolioQuantity(portfolioQuantity)
                .portfolioAveragePrice(portfolioAveragePrice)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    public Money calculateTotalAmount() {
        return new Money(portfolioQuantity.getQuantityValue() * portfolioAveragePrice.getMoneyValue());
    }

    /** 양방향 편의 메서드 **/
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public void addQuantity(Quantity addedQuantity, Money executedPrice) {
        long currentQty = this.portfolioQuantity.getQuantityValue();
        long newQty = addedQuantity.getQuantityValue();

        long totalQty = currentQty + newQty;

        long currentAmount = this.portfolioAveragePrice.getMoneyValue() * currentQty;
        long newAmount = executedPrice.getMoneyValue() * newQty;

        long updatedAvgPrice = (currentAmount + newAmount) / totalQty;

        this.portfolioQuantity = new Quantity(totalQty);
        this.portfolioAveragePrice = new Money(updatedAvgPrice);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public boolean decreaseQuantity(Quantity soldQuantity) {
        long currentQty = this.portfolioQuantity.getQuantityValue();
        long toSellQty = soldQuantity.getQuantityValue();

        if (toSellQty > currentQty) {
            throw new IllegalArgumentException("보유 수량보다 많이 팔 수 없습니다.");
        }

        long updatedQty = currentQty - toSellQty;
        this.portfolioQuantity = new Quantity(updatedQty);
        this.lastUpdatedAt = LocalDateTime.now();

        return updatedQty == 0;  // ❗ 수량이 0이면 true 반환 (삭제용)
    }
}

