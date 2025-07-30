package com.portfolio2025.first.domain.stock;

import com.portfolio2025.first.domain.PortfolioStock;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주식 종목을 관리하는 Stock
 * [07.26]
 * (수정) 기존 Embedded 필드에 AttributeOverride 추가
 * (수정) reserve -> assertReservable
 *
 * [고민]
 * 1. 유통 주식량은 현재 구체적인 기획이 없는 상황
 */
@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_category_id")
    private StockCategory stockCategory;

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "stock_price", nullable = false))
    private Money stockPrice;

    @Column(name = "stock_name", nullable = false, unique = true)
    private String stockName;

    @Column(name = "stock_code", nullable = false, unique = true)
    private String stockCode;

    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "available_quantity", nullable = false))
    private Quantity availableQuantity;

    @OneToMany(mappedBy = "stock")
    private List<PortfolioStock> portfolioStocks = new ArrayList<>();

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

    public void assertReservable(Quantity desiredQuantity) {
        validateSufficientQuantity(desiredQuantity);
    }

    public void decreaseAvailableQuantity(Quantity executedQuantity) {
        this.availableQuantity = availableQuantity.minus(executedQuantity);
    }
}

