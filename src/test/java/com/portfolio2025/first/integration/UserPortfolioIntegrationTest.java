package com.portfolio2025.first.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreatePortfolioRequestDTO;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.PortfolioService;
import com.portfolio2025.first.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserPortfolioIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioService portfolioService;

    private Long userId;

    @BeforeEach
    void setUp() {
        // 1) 기존 유저(예: user123) 있으면 사용, 없으면 생성
        User user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .userId("user123")
                                .name("홍길동")
                                .email("hong@test.com")
                                .location("서울")
                                .phoneNumber("010-1234-5678")
                                .build()
                ));

        userId = user.getId();
    }

    @Test
    @DisplayName("유저가 STOCK 포트폴리오를 성공적으로 생성한다")
    @Rollback(value = false)
    void createStockPortfolio() {
        // given
        CreatePortfolioRequestDTO dto = new CreatePortfolioRequestDTO(PortfolioType.STOCK);

        // when
        Portfolio portfolio = portfolioService.createPortfolio(userId, dto);

        // then
        assertThat(portfolio.getUser().getId()).isEqualTo(userId);
        assertThat(portfolio.getPortfolioType()).isEqualTo(PortfolioType.STOCK);
        assertThat(portfolio.getAvailableCash().getMoneyValue()).isEqualTo(0L);

        assertThat(portfolioRepository.findById(portfolio.getId())).isPresent();
    }

    @Test
    @DisplayName("동일한 PortfolioType 생성 시 예외 발생")
    void duplicatePortfolioException() {
        // given
        CreatePortfolioRequestDTO dto = new CreatePortfolioRequestDTO(PortfolioType.STOCK);

        // when & then
        assertThatThrownBy(() -> portfolioService.createPortfolio(userId, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 해당 유형의 포트폴리오가 존재합니다.");
    }
}
