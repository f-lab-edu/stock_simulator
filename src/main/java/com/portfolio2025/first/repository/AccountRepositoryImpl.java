package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Account;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl extends BaseRepositoryImpl<Account, Long> implements AccountRepository {

    public AccountRepositoryImpl(EntityManager em) {
        super(em, Account.class);
    }


    // 추가 조회 방식
    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String ql = "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber";
        List<Account> result = em.createQuery(ql, Account.class)
                .setParameter("accountNumber", accountNumber)
                .getResultList();
        return result.stream().findFirst();
    }
}

