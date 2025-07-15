package com.portfolio2025.first.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class InvestmentPreparationTest {

    @Test
    @DisplayName("투자 전 준비 단계: 유저, 계좌, 포트폴리오 생성")
    void testUserAccountPortfolioCreation() {
        // 1️⃣ User 생성 (static factory method 적용)
        User user = User.createUser(
                "홍길동",
                "서울",
                "010-1234-5678",
                "hong@example.com",
                "hong123"
        );

        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getPortfolios()).isEmpty();

        // 2️⃣ Account 생성
        Account account = Account.createAccount(
                user,
                "카카오뱅크",
                "1234567890",
                "홍길동"
        );

        assertThat(account.getAvailableCash().getMoneyValue()).isEqualTo(10_000_000L);
        assertThat(account.getUser()).isEqualTo(user);

        // 3️⃣ Portfolio 생성
        Portfolio portfolio = Portfolio.createPortfolio(
                user,
                PortfolioType.STOCK,
                LocalDateTime.now()
        );

        assertThat(portfolio.getAvailableCash().getMoneyValue()).isEqualTo(0L);
        assertThat(portfolio.getPortfolioTotalValue().getMoneyValue()).isEqualTo(0L);
        assertThat(portfolio.getUser()).isEqualTo(user);

        // 5️⃣ 검증
        assertThat(user.getPortfolios()).containsExactly(portfolio);
        assertThat(portfolio.getUser().getUserId()).isEqualTo("hong123");
        assertThat(account.getUser()).isSameAs(user);
    }
}
