package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioRepositoryImpl extends BaseRepositoryImpl<Portfolio, Long> implements PortfolioRepository {

    public PortfolioRepositoryImpl(EntityManager em) {
        super(em, Portfolio.class);}

    @Override
    public Optional<Portfolio> findByUserAndType(User user, PortfolioType portfolioType) {
        String jpql = "SELECT p FROM Portfolio p WHERE p.user = :user AND p.type = :type";

        List<Portfolio> result = em.createQuery(jpql, Portfolio.class)
                .setParameter("user", user)
                .setParameter("type", portfolioType)
                .getResultList();

        return result.stream().findFirst();
    }
}
