package com.portfolio2025.first.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Money;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Mockito 활용한 테스트 환경
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Money money;
    private Account account;

    // User 생성 관련 테스트 코드
    @BeforeEach()
    void setUp() {
        user = User.builder()
                .name("홍길동")
                .location("서울")
                .phoneNumber("010-1234-5678")
                .email("hong@test.com")
                .userId("user123")
                .build();

        account = Account.builder()
                .accountNumber("111111-11-111111")
                .bankName("국민은행")
                .user(user)
                .userName(user.getName())
                .build();

        money = new Money(2000L);
    }

    @Test
    void 계좌생성_성공() {
        // given
        String bankName = "KB국민은행";
        String accountNumber = "123-456-7890";
        String userName = "홍길동";

        Account savedAccount = Account.builder()
                .user(user)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .userName(userName)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // when
        Account result = accountService.createAccount(user, bankName, accountNumber, userName);

        // then
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getBankName()).isEqualTo(bankName);
        assertThat(result.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(result.getUserName()).isEqualTo(userName);
    }

    @Test
    void 계좌생성_실패_user_null() {
        assertThatThrownBy(() -> accountService.createAccount(null, "은행", "123", "이름"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자는 필수입니다.");
    }

    @Test
    void 계좌생성_실패_bankName_null() {
        assertThatThrownBy(() -> accountService.createAccount(user, null, "123", "이름"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("은행명은 필수입니다.");
    }

    @Test
    void 계좌생성_실패_bankName_blank() {
        assertThatThrownBy(() -> accountService.createAccount(user, " ", "123", "이름"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("은행명은 필수입니다.");
    }

    @Test
    void 계좌생성_실패_accountNumber_null() {
        assertThatThrownBy(() -> accountService.createAccount(user, "은행", null, "이름"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("계좌번호는 필수입니다.");
    }

    @Test
    void 계좌생성_실패_accountNumber_blank() {
        assertThatThrownBy(() -> accountService.createAccount(user, "은행", " ", "이름"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("계좌번호는 필수입니다.");
    }

    @Test
    void deposit_success() {
        // Given
        when(accountRepository.findByAccountNumber("123-456-7890")).thenReturn(Optional.of(account));
        Money depositMoney = new Money(1000L);

        // When
        accountService.deposit("123-456-7890", depositMoney);

        // Then
        assertEquals(99_999_999_000L, account.getAvailableCash().getAmount());
        assertEquals(1000L, user.getBalance().getAmount());

        verify(accountRepository, times(1)).findByAccountNumber("123-456-7890");
    }

    @Test
    void deposit_failed_throw_IllegalException() {
        // Given
        when(accountRepository.findByAccountNumber("123-456-7890")).thenReturn(Optional.of(account));
        Money depositMoney = new Money(1000_000_000_000_000L);

        // Then
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit("123-456-7890", depositMoney));
        assertEquals("잔액이 부족합니다", exception.getMessage());

        verify(accountRepository, times(1)).findByAccountNumber("123-456-7890");
    }

    @Test
    void withdraw_success() {
        // Given
        user.chargeBalance(new Money(5000L));
        when(accountRepository.findByAccountNumber("123-456-7890")).thenReturn(Optional.of(account));

        Money withdrawAmount = new Money(2000L);

        // When
        accountService.withdraw("123-456-7890", withdrawAmount);

        // Then
        assertEquals(100_000_002_000L, account.getAvailableCash().getAmount());
        assertEquals(3000L, user.getBalance().getAmount());

        verify(accountRepository, times(1)).findByAccountNumber("123-456-7890");
    }

    @Test
    void withdraw_failed_throw_IllegalException() {
        // Given
        when(accountRepository.findByAccountNumber("123-456-7890")).thenReturn(Optional.of(account));
        Money depositMoney = new Money(1000_000_000_000_000L);

        // Then
        String exceptionMessage = Assertions.assertThrows(IllegalArgumentException.class,
                        () -> accountService.withdraw("123-456-7890", depositMoney))
                .getMessage();
        assertEquals("잔액이 부족합니다", exceptionMessage);

        verify(accountRepository, times(1)).findByAccountNumber("123-456-7890");
    }
}

