package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import java.util.Optional;

public interface PortfolioRepository extends BaseRepository<Portfolio, Long>{
    Optional<Portfolio> findByUserAndType(User user, PortfolioType portfolioType);

    Optional<Portfolio> findByIdForUpdate(Long portfolioId);

    Optional<Portfolio> findByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType);

    boolean existsByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType);
}
