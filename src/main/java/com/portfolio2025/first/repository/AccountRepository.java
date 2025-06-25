package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Account;
import java.util.Optional;

public interface AccountRepository extends BaseRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
}

