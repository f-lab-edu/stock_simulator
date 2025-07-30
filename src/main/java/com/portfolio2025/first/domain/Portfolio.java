package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PortfolioStock 관리하는 상위 정보 Portfolio
 *
 * [07.26]
 * (수정) releaseAndDeductCash 내 차감은 진행하지 않음, 추가로 naming 수정함
 * (수정) 객체 생성 시 필요없는 인자 부분 제외
 *
 * [고민]
 * 1. 매수 주문을 넣을 때 실제 사용 금액을 차감하고 진행할지 혹은 체결되고 난 이후에 차감을 해야 하는지를 고민함.
 */
@Entity
@Getter
@Table(name = "portfolios", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "portfolio_type"}))
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 연관 (다대일)

    @Enumerated(EnumType.STRING)
    @Column(name = "portfolio_type", nullable = false)
    private PortfolioType portfolioType; // 포트폴리오 유형 (실전, 모의 등)

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "portfolio_total_value", nullable = false))
    private Money portfolioTotalValue; // 총 평가금액 (현금 + 주식 현재가 기준)

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "available_cash", nullable = false))
    private Money availableCash; // 거래 가능한 현금

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "reserved_cash", nullable = false))
    private Money reservedCash; // 매수 주문 시 예약된 금액

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 포트폴리오 생성일/수정일

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioStock> portfolioStocks = new ArrayList<>(); // PortfolioStock 연관 (보유 주식 내역)

    @Builder
    private Portfolio(User user, PortfolioType portfolioType) {
        this.user = user;
        this.portfolioType = portfolioType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.reservedCash = new Money(0L);
        this.availableCash = new Money(0L);
        this.portfolioTotalValue = new Money(0L);
    }

    public static Portfolio createPortfolio(User user, PortfolioType portfolioType) {
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .portfolioType(portfolioType)
                .build();

        // 양방향 편의 메서드
        user.addPortfolio(portfolio);
        return portfolio;
    }

    public void deposit(Money amount) {
        this.availableCash = availableCash.plus(amount);
    }

    public void withdraw(Money amount) {
        this.availableCash = availableCash.minus(amount);
    }

    private void validateSufficientCash(Money totalPrice) {
        if (availableCash.isLowerThan(totalPrice)) {
            throw new IllegalArgumentException("보유 현금이 부족하여 주문을 예약할 수 없습니다.");
        }
    }

    public void reserveAndDeductCash(Money amount) {
        validateSufficientCash(amount);
        this.availableCash = this.availableCash.minus(amount);
        this.reservedCash = this.reservedCash.plus(amount);
    }

    public void cancelReservedCash(Money amount) {
        this.reservedCash = this.reservedCash.minus(amount);
//        this.availableCash = this.availableCash.minus(amount);
    }

    // 매칭에 실패한 경우 -> Batch로 처리하는 방식도 생각해봐야 함
    public void cancelReservation(Money amount) {
        this.reservedCash = this.reservedCash.minus(amount);
        this.availableCash = this.availableCash.plus(amount);
    }
}

