package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Money;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.repository.AccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private void validateAccountCreateInput(User user, String bankName, String accountNumber) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("은행명은 필수입니다.");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다.");
        }
    }

    // 계좌 생성
    public Account createAccount(User user, String bankName, String accountNumber, String userName) {
        validateAccountCreateInput(user, bankName, accountNumber);

        Account account = Account.builder()
                .user(user)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .userName(userName)
                .build();

        return accountRepository.save(account);
    }
    
    // username(String)으로 Account 조회
    public List<Account> getAccountsByUsername(String username) {
        return accountRepository.findByUser(username);
    }

    // 입금
    // Transaction 단위에서 진행되어야 함
    @Transactional
    public void deposit(String accountNumber, Money money) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        User user = account.getUser();

        account.withdraw(money);
        user.deposit(money);
    }

    // 출금
    // Transaction 단위에서 진행되어야 함
    @Transactional
    public void withdraw(String accountNumber, Money money) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        User user = account.getUser();

        account.deposit(money);
        user.withdraw(money);
    }

}
