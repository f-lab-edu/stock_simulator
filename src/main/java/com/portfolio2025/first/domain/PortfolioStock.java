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

    // 보유하고 있는 특정 종목의 수량
    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "portfolio_quantity", nullable = false))
    private Quantity portfolioQuantity;

    // 예약 수량 (중복 매도 수량 방지 위함)
    @Embedded
    @AttributeOverride(name = "quantityValue", column = @Column(name = "reserved_quantity", nullable = false))
    private Quantity reservedQuantity = new Quantity(0L);

    @Embedded
    @AttributeOverride(name = "priceValue", column = @Column(name = "portfolio_average_price", nullable = false))
    private Money portfolioAveragePrice;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt; // 마지막 업데이트 한 시각 (Timeout 용도로 설정 가능함)

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

    public boolean decreaseQuantity(Quantity executed) {
        if (executed.getQuantityValue() > this.portfolioQuantity.getQuantityValue()) {
            throw new IllegalArgumentException("보유 수량보다 많이 차감할 수 없습니다.");
        }

        this.portfolioQuantity = this.portfolioQuantity.minus(executed);
        this.reservedQuantity = this.reservedQuantity.minus(executed);
        this.lastUpdatedAt = LocalDateTime.now();

        return this.portfolioQuantity.isZero();
    }

    public boolean hasNotEnough(Quantity otherQuantity) {
        return portfolioQuantity.isLowerThan(otherQuantity);
    }

    // 이중 매도 방지하기 위한 reserve 메서드
    public void reserve(Quantity requested) {
        // 1. 먼저 현재 거래 가능한 수량과 예약 수량을 비교함 - 거래가 지속적으로 가능한 상태인지 먼저 확인한다
        Quantity available = this.portfolioQuantity.minus(this.reservedQuantity);

        // 2. 거래 가능한 수량(존재 수량 - 예약 수량)과 requested 비교하기 때문에 문제 발생하지 않음
        if (available.isLowerThan(requested)) {
            throw new IllegalArgumentException("예약할 수 있는 수량이 부족합니다. [보유: "
                    + this.portfolioQuantity.getQuantityValue() + ", 예약됨: "
                    + this.reservedQuantity.getQuantityValue() + ", 요청: "
                    + requested.getQuantityValue() + "]");
        }

        this.reservedQuantity = this.reservedQuantity.plus(requested);
        this.lastUpdatedAt = LocalDateTime.now();
    }
}

