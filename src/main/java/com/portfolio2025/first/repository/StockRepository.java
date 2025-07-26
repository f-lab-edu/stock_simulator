package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.stock.Stock;
import java.util.Optional;

public interface StockRepository extends BaseRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);
}
