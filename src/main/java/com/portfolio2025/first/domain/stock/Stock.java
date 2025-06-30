package com.portfolio2025.first.domain.stock;

import com.portfolio2025.first.domain.PortfolioStock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Table(name = "stocks")
@Getter
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 StockCategory 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_category_id", nullable = false)
    private StockCategory stockCategory;

    @Column(name = "stock_price", nullable = false)
    private Long stockPrice;

    @Column(name = "stock_name", nullable = false, unique = true)
    private String stockName;

    @Column(name = "available_quantity", nullable = false)
    private Long availableQuantity;

    // 📦 (선택) 포트폴리오 종목 연관
    @OneToMany(mappedBy = "stock")
    private List<PortfolioStock> portfolioStocks = new ArrayList<>();

    // 📦 (선택) 주문 내역 연관
    @OneToMany(mappedBy = "stock")
    private List<StockOrder> stockOrders = new ArrayList<>();

    public boolean hasLowerQuantityThan(Long desiredQuantity) {
        return (availableQuantity < desiredQuantity) ? true : false;
    }

    public void decreaseQuantity(Long availableQuantity) {
        this.availableQuantity = this.availableQuantity - availableQuantity;
    }
}

