package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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

    @Override
    public Optional<Portfolio> findByIdForUpdate(Long portfolioId) {
        String jpql = "SELECT p FROM Portfolio p WHERE p.id = :portfolioId";

        List<Portfolio> result = em.createQuery(jpql, Portfolio.class)
                .setParameter("portfolioId", portfolioId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        return result.stream().findFirst();
    }

    @Override
    public Optional<Portfolio> findByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType) {
        String jpql = "SELECT p FROM Portfolio p WHERE p.user.id = :userId AND p.portfolioType = :type";

        List<Portfolio> result = em.createQuery(jpql, Portfolio.class)
                .setParameter("userId", userId)
                .setParameter("type", portfolioType)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        return result.stream().findFirst();
    }

//    public boolean existsByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType) {
//        String jpql = "SELECT 1 FROM Portfolio p WHERE p.user.id = :userId AND p.portfolioType = :type";
//
//        List<Integer> result = em.createQuery(jpql, Integer.class)
//                .setParameter("userId", userId)
//                .setParameter("type", portfolioType)
//                .setMaxResults(1)
//                .getResultList();
//
//        return !result.isEmpty();
//    }

    @Override
    public boolean existsByUserIdAndPortfolioType(Long userId, PortfolioType portfolioType) {
        String jpql = "SELECT COUNT(p) FROM Portfolio p WHERE p.user.id = :userId AND p.portfolioType = :type";

        Long count = em.createQuery(jpql, Long.class)
                .setParameter("userId", userId)
                .setParameter("type", portfolioType)
                .getSingleResult();

        return count > 0;
    }
}
