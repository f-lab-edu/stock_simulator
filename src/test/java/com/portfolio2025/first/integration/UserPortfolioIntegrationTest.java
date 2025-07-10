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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test") // application-test.yml 설정을 적용
@Transactional()
class UserPortfolioIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioService portfolioService;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.createUser(
                "John Doe",
                "New York",
                "010-1234-5678",
                "john.doe@email.com",
                "john123"
        );
        userRepository.save(user);
        userId = user.getId();
    }

    @Test
    @DisplayName("유저가 STOCK 포트폴리오를 성공적으로 생성한다")
    void createStockPortfolio() {
        // given
        CreatePortfolioRequestDTO dto = new CreatePortfolioRequestDTO(PortfolioType.STOCK);

        // when
        Portfolio portfolio = portfolioService.createPortfolio(userId, dto);

        // then
        assertThat(portfolio.getUser().getId()).isEqualTo(userId);
        assertThat(portfolio.getPortfolioType()).isEqualTo(PortfolioType.STOCK);
        assertThat(portfolio.getAvailableCash().getMoneyValue()).isEqualTo(0L);
    }

    @Test
    @DisplayName("동일한 PortfolioType 생성 시 예외 발생")
    void duplicatePortfolioException() {
        // given
        CreatePortfolioRequestDTO dto = new CreatePortfolioRequestDTO(PortfolioType.STOCK);
        portfolioService.createPortfolio(userId, dto);

        // when & then
        assertThatThrownBy(() -> portfolioService.createPortfolio(userId, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 해당 유형의 포트폴리오가 존재합니다.");
    }
}
