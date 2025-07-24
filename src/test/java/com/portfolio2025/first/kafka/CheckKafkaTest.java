package com.portfolio2025.first.kafka;

import com.portfolio2025.first.consumer.OrderRequestConsumer;
import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.CreateAccountRequestDTO;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.TransferToPortfolioRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.TradeRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.RedisStockOrderService;
import com.portfolio2025.first.service.old.BuyStockService;
import com.portfolio2025.first.service.old.SellStockService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(
        properties = {
                "spring.kafka.bootstrap-servers=localhost:9092"
        }
)
@ActiveProfiles("test")
@Transactional
@Rollback(value = false)
@Async
@EnableKafka
public class CheckKafkaTest {

    @Autowired
    private BuyStockService buyStockService;
    @Autowired private SellStockService sellStockService;
    @Autowired private RedisStockOrderService redisStockOrderService;
    @Autowired private StockRepository stockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountService accountService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private PortfolioStockRepository portfolioStockRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private OrderRequestConsumer orderRequestConsumer;

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private StringRedisTemplate redisTemplate;

    private User userA;
    private User userB;
    private Stock samsung;
    private Stock naver;

    private Account accountA;
    private Account accountB;

    private Portfolio buyPortfolio;
    private Portfolio sellPortfolio;

    @BeforeEach
    void setUp() {
        /** userA - 매수자 **/
        userA = userRepository.save(User.createUser("sunghun", "suwon",
                "010-1234-5678", "test@email.com", "pw"));
        CreateAccountRequestDTO dtoA =
                new CreateAccountRequestDTO("kookmin", "111111-22-333333", "sunghun");
        // 1000만원 자동 생성된 상황
        accountA = accountService.createAccount(userA.getId(), dtoA);
        // 유저에게 포트폴리오 생성
        portfolioRepository.save(Portfolio.createPortfolio(userA, PortfolioType.STOCK, LocalDateTime.now()));
        buyPortfolio = portfolioRepository.findByUserIdAndPortfolioType(userA.getId(), PortfolioType.STOCK)
                .orElseThrow();
        // 계좌에서 400만원 인출, 투자금 400만원인 상황 (userA - 투자금 400만원)
        accountService.transferFromAccountToPortfolio(userA.getId(), new TransferToPortfolioRequestDTO(
                "111111-22-333333",
                this.buyPortfolio.getId(),
                4000_000L));

        /** userB - 매도자 **/
        userB = userRepository.save(User.createUser("sungsung", "seoul",
                "010-5678-0000", "test@naver.com", "pw12345"));
        CreateAccountRequestDTO dtoB =
                new CreateAccountRequestDTO("sinhan", "444444-55-666666", "sungsung");
        // 1000만원 자동 생성된 상황
        accountB = accountService.createAccount(userB.getId(), dtoB);
        // 유저에게 포트폴리오 생성
        portfolioRepository.save(Portfolio.createPortfolio(userB, PortfolioType.STOCK, LocalDateTime.now()));
        sellPortfolio = portfolioRepository.findByUserIdAndPortfolioType(userB.getId(), PortfolioType.STOCK)
                .orElseThrow();
        // 계좌에서 500만원 인출, 투자금 500만원인 상황 (userB - 투자금 500만원)
        accountService.transferFromAccountToPortfolio(userB.getId(), new TransferToPortfolioRequestDTO(
                "444444-55-666666",
                sellPortfolio.getId(),
                5000_000L));

        // 주식 종목 정보
        samsung = stockRepository.save(Stock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .stockPrice(new Money(100_000L)) // 10만원 짜리
                .availableQuantity(new Quantity(1_000_000L)) // 100만 개 수량 가지고 있는 상황
                .build());

        naver = stockRepository.save(Stock.builder()
                .stockCode("007890")
                .stockName("네이버")
                .stockPrice(new Money(150_000L)) // 15만원 짜리
                .availableQuantity(new Quantity(2_000_000L)) // 200만 개 수량 가지고 있는 상황
                .build());

        stockRepository.save(samsung);
        stockRepository.save(naver);
    }

    @BeforeEach
    void resetKafkaTopic() throws Exception {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }


    @Test
    void testBuyOrdersProduceAndConsumeKafkaEvents() throws Exception {
        // ✅ 테스트할 주문 수 (Kafka 메시지 수)
        int numberOfOrders = 1;

        // ✅ CountDownLatch 설정: 메시지 수만큼 설정
//        OrderPrepareConsumer.latch = new CountDownLatch(numberOfOrders);

        // ✅ 반복적으로 매수 주문 실행
        for (int i = 0; i < numberOfOrders; i++) {
            StockOrderRequestDTO dto = new StockOrderRequestDTO(
                    samsung.getStockCode(),
                    1L,  // 1주씩
                    100_000L + i * 1000, // 매번 조금씩 다른 가격
                    userA.getId()
            );

            buyStockService.placeSingleBuyOrder(dto);
            Thread.sleep(10000); // Kafka listener보다 DB가 먼저 반영되게 잠깐 대기
        }

//        boolean completed = OrderPrepareConsumer.latch.await(5, TimeUnit.SECONDS);
//        assertTrue(completed, numberOfOrders + "건의 Kafka 메시지 중 일부가 소비되지 않았습니다.");

//        OrderPrepareConsumer.latch = null;
    }


}
