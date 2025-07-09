package com.portfolio2025.first.service;


import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreatePortfolioRequestDTO;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    @Transactional
    public Portfolio createPortfolio(Long userId, CreatePortfolioRequestDTO dto) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        PortfolioType portfolioType = dto.getPortfolioType();

        Portfolio portfolio = Portfolio.createPortfolio(user, portfolioType, LocalDateTime.now());
        return portfolioRepository.save(portfolio);
    }
}
