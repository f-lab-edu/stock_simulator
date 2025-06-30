package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.User;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends BaseRepository<Account, Long> {
    // 계좌번호로 탐색하는 방식
    Optional<Account> findByAccountNumber(String accountNumber);

    // Pessimistic Lock을 적용한 경우
    Optional<Account> findByAccountNumberWithLock(String accountNumber);

    // 사용자 이름으로 계좌 조회하는 방식
    List<Account> findByUserName(String username);

    // 사용자 객체로 계좌 조회하는 방식
    List<Account> findByUser(User user);
}

