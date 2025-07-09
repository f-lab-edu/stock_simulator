package com.portfolio2025.first.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreatePortfolioRequestDTO;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private User testUser;
    private Portfolio testPortfolio;
    private CreatePortfolioRequestDTO createPortfolioDTO;

    private final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("홍길동")
                .location("서울")
                .phoneNumber("010-1234-5678")
                .email("hong@test.com")
                .userId("user123")
                .build();

        testPortfolio = Portfolio.builder()
                .user(testUser)
                .portfolioType(PortfolioType.STOCK)
                .build();

        createPortfolioDTO = new CreatePortfolioRequestDTO();
        createPortfolioDTO.setPortfolioType(PortfolioType.STOCK);
    }

    @Test
    void 포트폴리오_생성_성공() {
        // given
        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // when
        Portfolio result = portfolioService.createPortfolio(TEST_USER_ID, createPortfolioDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getPortfolioType()).isEqualTo(PortfolioType.STOCK);
    }
}

