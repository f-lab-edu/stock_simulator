package com.portfolio2025.first.service;


import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreatePortfolioRequestDTO;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매수, 매도 주문 생성을 담당하는 StockOrderService
 *
 * [08.13]
 * (수정)
 * createPortfolio - DB 비관락 적용에서 DB Unique 제약 조건을 이미 가지고 있는걸로 대체. 중복 생성 요청을 방지할 수 있다!
 *
 * [고민]
 * 1. Transaction 범위 추가로 Redisson 락 혹은 DB 락을 어떻게 적절하게 배정할 수 있는지 고민하기 -> DB Unique 제약 활용으로 해결
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    @Transactional
    public Portfolio createPortfolio(Long userId, CreatePortfolioRequestDTO dto) {
        try {
            if (portfolioRepository.existsByUserIdAndPortfolioType(userId, dto.getPortfolioType())) {
                throw new IllegalStateException("이미 해당 유형의 포트폴리오가 존재합니다.");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

            Portfolio p = Portfolio.createPortfolio(user, dto.getPortfolioType());
            // flush() 함으로써 DB Unique 제약 검증으로 바로 이어질 수 있게 설정함
            return portfolioRepository.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) { // 유니크 제약 위반
            throw new IllegalStateException("이미 해당 유형의 포트폴리오가 존재합니다.", e);
        }
    }

    // 유저에 존재하는 포트폴리오 조회하기
    public Portfolio findPortfolioWithLock(Long userId, PortfolioType portfolioType) {
        return portfolioRepository.findByUserIdAndPortfolioType(userId, portfolioType)
                .orElseThrow(() -> new IllegalArgumentException("해당 포트폴리오가 존재하지 않습니다."));
    }


}
