package com.portfolio2025.first.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio2025.first.RedisRegister;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.TransferToPortfolioRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.BuyOrderProcessor;
import com.portfolio2025.first.service.SellOrderProcessor;
import com.portfolio2025.first.service.StockOrderService;
import com.portfolio2025.first.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class BuyStockServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired private StockOrderService stockOrderService;
    @Autowired private AccountService accountService;
    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private RedisRegister redisRegister;
    @Autowired private BuyOrderProcessor buyOrderProcessor;
    @Autowired private SellOrderProcessor sellOrderProcessor;


    /**
     * 테스트마다 독립적인 consumer group 을 사용해 오프셋/중복소비 간섭을 방지.
     */
    @DynamicPropertySource
    static void overrideKafkaGroupId(DynamicPropertyRegistry r) {
        r.add("spring.kafka.consumer.group-id",
                () -> "order-prepare-group-test-" + UUID.randomUUID());
    }

    @Test
    @DisplayName("매수주문 E2E: DB 저장 → Kafka 발행 → Listener 소비 → Redis 마킹까지")
    void 매수주문_성공_E2E() {
        // given
        long userId = 1L;
        String stockCode = "005930";
        long qty = 2L;
        long price = 100_000L;

        accountService.transferFromAccountToPortfolio(userId,
                new TransferToPortfolioRequestDTO("111-222-333333", 1L, 2000_000L));

        StockOrderRequestDTO dto = new StockOrderRequestDTO(
                stockCode,
                qty,
                price,
                userId
        );

        // when: 주문 생성 (내부에서 트랜잭션 커밋 → 이벤트 발행 동기 send().get())
        stockOrderService.placeSingleOrder(dto, buyOrderProcessor);

        // then 1) DB에 주문이 저장되었는지
        var all = orderRepository.findAll();
        assertThat(all).hasSize(1);
        Order saved = all.get(0);
        assertThat(saved.getId()).isEqualTo(1L);

        // then 2) Listener가 소비한 후 남기는 확실한 사이드 이펙트(= Redis processed 마킹)를 기다린다.
        Awaitility.await()
                .atMost(Duration.ofSeconds(8))        // CI 환경이면 10~15초로 늘려도 OK
                .pollInterval(Duration.ofMillis(150))
                .untilAsserted(() ->
                        assertThat(redisRegister.isAlreadyProcessed(saved)).isTrue()
                );
    }
}
