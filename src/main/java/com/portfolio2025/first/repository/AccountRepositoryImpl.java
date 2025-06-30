package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl extends BaseRepositoryImpl<Account, Long> implements AccountRepository {

    public AccountRepositoryImpl(EntityManager em) {
        super(em, Account.class);
    }

    // 계좌번호 조회 (Read-only)
    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String ql = "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber";
        List<Account> result = em.createQuery(ql, Account.class)
                .setParameter("accountNumber", accountNumber)
                .getResultList();
        return result.stream().findFirst();
    }

    // 계좌번호 조회 w/pessimistic lock
    @Override
    public Optional<Account> findByAccountNumberWithLock(String accountNumber) {
        String ql = "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber";
        List<Account> result = em.createQuery(ql, Account.class)
                .setParameter("accountNumber", accountNumber)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)  // ✅ 비관적 락 적용
                .getResultList();
        return result.stream().findFirst();
    }


    // 동명이인이 생길 수 있다
    // 1. String username이 아니라 로그인 세션 정보를 활용하는 방안
    // 2. String username + String email 정보, 즉 복합키를 활용해서 userId를 조회하는 방식
    @Override
    public List<Account> findByUserName(String username) {
        String ql = "SELECT a FROM Account a JOIN a.user u WHERE u.name = :username";
        return em.createQuery(ql, Account.class)
                .setParameter("username", username)
                .getResultList();
    }

    // User 조회 (동명이인 문제 X)
    @Override
    public List<Account> findByUser(@Param("user") User user) {
        return em.createQuery("SELECT a FROM Account a WHERE a.user = :user", Account.class)
                .setParameter("user", user)
                .getResultList();
    }


}

