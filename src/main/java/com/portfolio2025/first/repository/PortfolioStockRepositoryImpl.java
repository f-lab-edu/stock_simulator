package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.stock.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioStockRepositoryImpl extends BaseRepositoryImpl <PortfolioStock, Long> implements PortfolioStockRepository {
    public PortfolioStockRepositoryImpl(EntityManager em) {
        super(em, PortfolioStock.class);
    }

    // PESSIMISTIC_WRITE - DB 락 반영함
    @Override
    public Optional<PortfolioStock> findByPortfolioAndStock(Portfolio portfolio, Stock stock) {
        String jqpl = "select ps from PortfolioStock ps where "
                + "ps.portfolio = :portfolio and ps.stock = :stock";

        List<PortfolioStock> resultList = em.createQuery(jqpl, PortfolioStock.class)
                .setParameter("portfolio", portfolio)
                .setParameter("stock", stock)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        return resultList.stream().findFirst();
    }
}
