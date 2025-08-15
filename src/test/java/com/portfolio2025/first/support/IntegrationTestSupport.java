package com.portfolio2025.first.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestSupport {
    @Autowired
    private org.flywaydb.core.Flyway flyway;

    @BeforeAll
    void resetDB() {
        flyway.clean();   // 모든 테이블 삭제
        flyway.migrate(); // db/migration + db/testdata 실행
    }
}
