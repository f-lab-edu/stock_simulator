package com.portfolio2025.first.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreateAccountRequestDTO;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 MySQL을 실행시킨 상황에서 진행하는 방식임
 */

@SpringBootTest  // 스프링 전체 빈 로딩 (실제 MySQL 통합해서 진행)
@Transactional   // 테스트 끝나면 자동 롤백 (DB 깨끗하게 유지)
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 계좌생성_성공_DB() {
        // given
        User user = User.builder()
                .name("홍길동")
                .location("서울")
                .phoneNumber("010-1234-5678")
                .email("hong@test.com")
                .userId("user123")
                .build();
        userRepository.save(user);  // ✅ 진짜 DB에 저장됨

        String bankName = "신한은행";
        String accountNumber = "111-222-333";
        String userName = "홍길동";

        CreateAccountRequestDTO createAccountRequestDTO = new CreateAccountRequestDTO(bankName, accountNumber,
                userName);

        // when
        Account savedAccount = accountService.createAccount(user.getId(), createAccountRequestDTO);

        // then
        Account foundAccount = accountRepository.findById(savedAccount.getId()).orElseThrow();

        assertThat(foundAccount.getUser().getId()).isEqualTo(user.getId());
        assertThat(foundAccount.getBankName()).isEqualTo(bankName);
        assertThat(foundAccount.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(foundAccount.getUserName()).isEqualTo(userName);
    }
}
