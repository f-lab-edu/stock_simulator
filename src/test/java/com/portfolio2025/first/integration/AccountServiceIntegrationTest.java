package com.portfolio2025.first.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.dto.CreateAccountRequestDTO;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.support.IntegrationTestSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * [테스트] 계좌 생성 관련 테스트
 * -> 문제가 발생할 수 있는 상황은???
 * 1. 사용자 생성 중복 요청을 시도하는 경우
 * 2. DTO 누락이 발생한 경우
 */
class AccountServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void cleanDB() throws Exception {
        var conn = DataSourceUtils.getConnection(dataSource);
        try (var st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String t : new String[]{
                    "accounts","users","orders","trade","portfolios","portfolio_stocks"
            }) st.execute("TRUNCATE TABLE " + t);
            st.execute("SET FOREIGN_KEY_CHECKS=1");
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Test
    @Transactional
    @Rollback(value = false)
    void 계좌생성_성공_DB() {
        // given
        User user = User.builder()
                .userId("user123")
                .name("Hong Gil Dong")
                .email("hong@test.com")
                .location("Suwon")
                .phoneNumber("010-1234-5678")
                .build();
        userRepository.save(user);

        CreateAccountRequestDTO req =
                new CreateAccountRequestDTO("신한은행", "444-555-666666", "홍길동");

        // when
        Account saved = accountService.createAccount(user.getId(), req);
        // then
        Account found = accountRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getBankName()).isEqualTo("신한은행");
        assertThat(found.getAccountNumber()).isEqualTo("444-555-666666");
        assertThat(found.getUserName()).isEqualTo("홍길동");
    }
}
