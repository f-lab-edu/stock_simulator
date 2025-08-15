package com.portfolio2025.first.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Testcontainers // 테스트 시작/끝에 컨테이너 자동 관리
@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrationTest {

    // MySQL 컨테이너 생성 (동적 생성 및 삭제까지 진행함)
    @Container
    protected static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withReuse(true)
            .withDatabaseName("testdb")     // 컨테이너 내부 DB 이름
            .withUsername("testuser")       // 접속 계정
            .withPassword("testpass");      // 접속 비밀번호

    // 컨테이너 환경 → Spring Boot 프로퍼티 주입
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Test
    void contextLoads() {
        // DB 연결이 정상적으로 되는지 확인하는 기초 테스트
    }
}
