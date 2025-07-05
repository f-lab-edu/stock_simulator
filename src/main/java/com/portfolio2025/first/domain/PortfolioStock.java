package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.stock.Stock;
import jakarta.persistence.Column;
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
import lombok.NoArgsConstructor;

@Entity
@Table(name = "portfolio_stocks")
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

    @Column(name = "portfolio_quantity", nullable = false)
    private Long portfolioQuantity;

    @Column(name = "portfolio_average_price", nullable = false)
    private Long portfolioAveragePrice;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Builder
    public PortfolioStock(Long id, Portfolio portfolio, Stock stock, Long portfolioQuantity,
                          Long portfolioAveragePrice, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.portfolio = portfolio;
        this.stock = stock;
        this.portfolioQuantity = portfolioQuantity;
        this.portfolioAveragePrice = portfolioAveragePrice;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    // static 생성자
    public static PortfolioStock create(Portfolio portfolio, Stock stock,
                                        Long portfolioAveragePrice, LocalDateTime createdAt) {
        return PortfolioStock.builder()
                .portfolio(portfolio)
                .stock(stock)
                .portfolioQuantity(0L)
                .portfolioAveragePrice(portfolioAveragePrice)
                .lastUpdatedAt(createdAt)
                .build();
    }

    public void addQuantity(Long quantity) {
        this.portfolioQuantity += quantity;
    }
}

