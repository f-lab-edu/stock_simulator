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

@Entity
@Table(name = "portfolio_stocks")
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
}

