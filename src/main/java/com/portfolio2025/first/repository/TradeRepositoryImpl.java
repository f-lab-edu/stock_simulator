package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Trade;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class TradeRepositoryImpl extends BaseRepositoryImpl<Trade, Long> implements TradeRepository {
    public TradeRepositoryImpl(EntityManager em) {
        super(em, Trade.class);
    }

    @Override
    public boolean existsByBuyOrderAndSellOrder(Long buyOrderId, Long sellOrderId) {
        String jpql = "SELECT COUNT(t) FROM Trade t WHERE t.buyOrder.id = :buyOrderId AND t.sellOrder.id = :sellOrderId";
        Long count = em.createQuery(jpql, Long.class)
                .setParameter("buyOrderId", buyOrderId)
                .setParameter("sellOrderId", sellOrderId)
                .getSingleResult();
        return count > 0;
    }
}
