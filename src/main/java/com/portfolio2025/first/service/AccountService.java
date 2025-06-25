package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public Account createAccount(User user, String bankName, String accountNumber, String userName) {
        // 생성 관련 검증을 모두 진행함
        validateAccountCreateInput(user, bankName, accountNumber);

        Account account = Account.builder()
                .user(user)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .userName(userName)
                .build();

        return accountRepository.save(account);
    }

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
}
