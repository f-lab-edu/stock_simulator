package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * Portfolio 저장소 역할을 수행하는 PortfolioRepository
 *
 * [07.30]
 * (수정) portfolio 조회 시 비관적 락 적용
 *
 * [고민]
 * 1. DTO 생성 중복 로직이 많이 발생하는 상황
 * 2. Transaction 범위 추가로 Redisson 락 혹은 DB 락을 어떻게 적절하게 배정할 수 있는지 고민하기
 */
public interface PortfolioRepository extends BaseRepository<Portfolio, Long>{
    Optional<Portfolio> findByUserAndType(User user, PortfolioType portfolioType);

    Optional<Portfolio> findByIdForUpdate(Long portfolioId);

    Optional<Portfolio> findByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType);

    boolean existsByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType);

    Optional<Portfolio> findByUserIdAndPortfolioTypeWithLock(Long userId, PortfolioType portfolioType);

    Portfolio saveAndFlush(Portfolio portfolio);
}
