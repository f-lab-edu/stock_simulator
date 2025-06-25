package com.portfolio2025.first.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("홍길동")
                .location("서울")
                .phoneNumber("010-1234-5678")
                .email("hong@test.com")
                .userId("user123")
                .build();
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
}

