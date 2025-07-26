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
        PortfolioType portfolioType = dto.getPortfolioType();

        // 중복 방지 + 추가로 try-catch 로 어떻게 방지할지도 고민해보기
        if (portfolioRepository.existsByUserIdAndPortfolioType(userId, portfolioType)) {
            throw new IllegalStateException("이미 해당 유형의 포트폴리오가 존재합니다.");
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        Portfolio portfolio = Portfolio.createPortfolio(user, portfolioType, LocalDateTime.now());
        return portfolioRepository.save(portfolio);
    }

    // 유저에 존재하는 포트폴리오 조회하기
    public Portfolio findPortfolioWithLock(Long userId, PortfolioType portfolioType) {
        return portfolioRepository.findByUserIdAndPortfolioType(userId, portfolioType)
                .orElseThrow(() -> new IllegalArgumentException("해당 포트폴리오가 존재하지 않습니다."));
    }


}
