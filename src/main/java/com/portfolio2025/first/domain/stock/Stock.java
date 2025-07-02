package com.portfolio2025.first.domain.stock;

import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 StockCategory 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_category_id")
    private StockCategory stockCategory;

    @Embedded
    @Column(name = "stock_price", nullable = false)
    private Money stockPrice;

    @Column(name = "stock_name", nullable = false, unique = true)
    private String stockName;

    @Column(name = "stock_code", nullable = false, unique = true)
    private String stockCode;

    @Embedded
    @Column(name = "available_quantity", nullable = false)
    private Quantity availableQuantity;

    // 📦 (선택) 포트폴리오 종목 연관
    @OneToMany(mappedBy = "stock")
    private List<PortfolioStock> portfolioStocks = new ArrayList<>();

    // 📦 (선택) 주문 내역 연관
    @OneToMany(mappedBy = "stock")
    private List<StockOrder> stockOrders = new ArrayList<>();

    @Builder
    private Stock(Long id, StockCategory stockCategory, Money stockPrice, String stockName,
                 String stockCode, Quantity availableQuantity) {
        this.id = id;
        this.stockCategory = stockCategory;
        this.stockPrice = stockPrice;
        this.stockName = stockName;
        this.stockCode = stockCode;
        this.availableQuantity = availableQuantity;
    }

    public static Stock createStock(StockCategory stockCategory, Money stockPrice,
                                    String stockName, String stockCode, Quantity availableQuantity) {
        return Stock.builder()
                .stockCategory(stockCategory)
                .stockPrice(stockPrice)
                .stockName(stockName)
                .stockCode(stockCode)
                .availableQuantity(availableQuantity)
                .build();
    }

    public boolean hasLowerQuantityThan(Quantity desiredQuantity) {
        return availableQuantity.isLowerThan(desiredQuantity);
    }

    public void validateSufficientQuantity(Quantity desiredQuantity) {
        if (hasLowerQuantityThan(desiredQuantity)) {
            throw new IllegalArgumentException("Not enough stock quantity in market");
        }
    }
}

