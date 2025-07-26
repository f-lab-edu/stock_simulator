package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Trade;

public interface TradeRepository extends BaseRepository<Trade, Long> {
    boolean existsByBuyOrderAndSellOrder(Long buyOrderId, Long sellOrderId);
}
