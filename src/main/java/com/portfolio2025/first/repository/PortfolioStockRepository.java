package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.stock.Stock;
import java.util.Optional;

public interface PortfolioStockRepository extends BaseRepository<PortfolioStock, Long> {
    // 추가 기능
    Optional<PortfolioStock> findByPortfolioAndStock(Portfolio portfolio, Stock stock);
}
