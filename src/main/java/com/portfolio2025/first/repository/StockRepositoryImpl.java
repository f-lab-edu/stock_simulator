package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.stock.Stock;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepositoryImpl extends BaseRepositoryImpl<Stock, Long> implements StockRepository{
    public StockRepositoryImpl(EntityManager em) {
        super(em, Stock.class);
    }

    @Override
    public Optional<Stock> findByStockCode(String stockCode) {
        String jpql = "SELECT s FROM Stock s where "
                + "s.stockCode = :stockCode";

        return em.createQuery(jpql, Stock.class)
                .setParameter("stockCode", stockCode)
                .getResultList()
                .stream().
                findFirst();
    }
}
