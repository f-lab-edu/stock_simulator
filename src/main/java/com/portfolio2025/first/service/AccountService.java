package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreateAccountRequestDTO;
import com.portfolio2025.first.dto.TransferToAccountRequestDTO;
import com.portfolio2025.first.dto.TransferToPortfolioRequestDTO;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계좌 관련 로직
 *
 */


@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;

    private void validateAccountCreateInput(User user, CreateAccountRequestDTO requestDTO) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }
        if (requestDTO.getBankName() == null || requestDTO.getBankName().isBlank()) {
            throw new IllegalArgumentException("은행명은 필수입니다.");
        }
        if (requestDTO.getAccountNumber() == null || requestDTO.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다.");
        }
    }

    // 계좌 생성
    @Transactional
    public Account createAccount(Long userId, CreateAccountRequestDTO requestDTO) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));
        validateAccountCreateInput(user, requestDTO); // 검증 위치는 계속해서 생각하기

        return accountRepository.save(Account.createAccount(user, requestDTO.getBankName(),
                requestDTO.getAccountNumber(), requestDTO.getUserName()));
    }
    
    // username(String)으로 Account 조회
    public List<Account> getAccountsByUsername(String username) {
        return accountRepository.findByUserName(username);
    }

    // Account -> Portfolio
    @Transactional
    public void transferFromAccountToPortfolio(Long userId, TransferToPortfolioRequestDTO dto) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));
        Account account = accountRepository.findByAccountNumberWithLock(dto.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(dto.getPortfolioId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        // 메서드화 하기
        validatePortfolioOwnership(portfolio, user);

        Money amount = dto.toMoney();
        account.withdraw(amount); // 계좌에서 인출하고
        portfolio.deposit(amount); // 포트폴리오에 입금
    }

    private void validatePortfolioOwnership(Portfolio portfolio, User user) {
        if (!portfolio.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 포트폴리오에 접근할 수 없습니다.");
        }
    }

    // Portfolio -> Account
    @Transactional
    public void transferFromPortfolioToAccount(Long userId, TransferToAccountRequestDTO dto) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));
        Account account = accountRepository.findByAccountNumberWithLock(dto.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(dto.getPortfolioId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        validatePortfolioOwnership(portfolio, user);

        Money amount = dto.toMoney();

        portfolio.withdraw(amount); // 포트폴리오에서 출금
        account.deposit(amount); // 계좌에 입금
    }

}
