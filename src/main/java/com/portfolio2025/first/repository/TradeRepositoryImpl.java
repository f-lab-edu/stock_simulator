package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Trade;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class TradeRepositoryImpl extends BaseRepositoryImpl<Trade, Long> implements TradeRepository {
    public TradeRepositoryImpl(EntityManager em) {
        super(em, Trade.class);
    }

}
