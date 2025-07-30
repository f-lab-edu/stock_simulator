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

/**
 * 매수, 매도 주문 생성을 담당하는 StockOrderService
 *
 * [07.30]
 * (수정)
 *
 * [고민]
 * 1. DTO 생성 중복 로직이 많이 발생하는 상황
 * 2. Transaction 범위 추가로 Redisson 락 혹은 DB 락을 어떻게 적절하게 배정할 수 있는지 고민하기
 */
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

        Portfolio portfolio = Portfolio.createPortfolio(user, portfolioType);
        return portfolioRepository.save(portfolio);
    }

    // 유저에 존재하는 포트폴리오 조회하기
    public Portfolio findPortfolioWithLock(Long userId, PortfolioType portfolioType) {
        return portfolioRepository.findByUserIdAndPortfolioType(userId, portfolioType)
                .orElseThrow(() -> new IllegalArgumentException("해당 포트폴리오가 존재하지 않습니다."));
    }


}
