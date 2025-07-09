package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * Portfolio - 잔고
 * Account - User - Portfolio 선행적으로 구성되어 있어야 함
 */


@Entity
@Getter
@Table(name = "portfolios")
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 연관 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 포트폴리오 유형 (실전, 모의 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortfolioType portfolioType;

    // 총 평가금액 (현금 + 주식 현재가 기준)
    @Column(name = "portfolio_total_value", nullable = false)
    private Money portfolioTotalValue;

    // 거래 가능한 현금
    @Column(name = "available_cash", nullable = false)
    private Money availableCash;

    // 포트폴리오 생성일/수정일
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // PortfolioStock 연관 (보유 주식 내역)
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioStock> portfolioStocks = new ArrayList<>();


    @Builder
    private Portfolio(User user, PortfolioType portfolioType, LocalDateTime updatedAt) {
        this.user = user;
        this.portfolioType = portfolioType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.availableCash = new Money(0L);
        this.portfolioTotalValue = new Money(0L);
    }

    public static Portfolio createPortfolio(User user, PortfolioType portfolioType, LocalDateTime updatedAt) {
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .portfolioType(portfolioType)
                .updatedAt(updatedAt)
                .build();

        // 양방향 편의 메서드
        user.addPortfolio(portfolio);
        return portfolio;
    }

    public void increaseCash(Money totalCost) {
        this.availableCash = availableCash.plus(totalCost);
    }

    public void increaseAvailableCash(Money amount) {
        this.availableCash = availableCash.plus(amount);
    }

    public void decreaseAvailableCash(Money amount) {
        this.availableCash = availableCash.minus(amount);
    }
}

