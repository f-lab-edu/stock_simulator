package com.portfolio2025.first.integration;


import static org.junit.jupiter.api.Assertions.assertTrue;

import com.portfolio2025.first.consumer.OrderPrepareConsumer;
import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
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
import com.portfolio2025.first.service.BuyStockService;
import com.portfolio2025.first.service.RedisStockOrderService;
import com.portfolio2025.first.service.SellStockService;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;


@SpringBootTest
@TestPropertySource(
        properties = {
                "spring.kafka.bootstrap-servers=localhost:9092"
        }
)
@ActiveProfiles("test")
@Transactional
@Rollback(value = false)
public class TradeIntegrationTest {

    @Autowired private BuyStockService buyStockService;
    @Autowired private SellStockService sellStockService;
    @Autowired private RedisStockOrderService redisStockOrderService;
    @Autowired private StockRepository stockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountService accountService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private PortfolioStockRepository portfolioStockRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private OrderPrepareConsumer orderPrepareConsumer;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

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


    /**
     *
     * [질문 1]
     * Q) 일시적으로 Order not Found 문제점이 발생하는 이유? :
     *    - 매수, 매도 주문 중간에 flush() 했음에도 간혹 Order not found 에러가 발생합니다. 왜 이럴까요??
     *
     * [질문 2]
     * Q) Kafka event (order.created)가 2번 정상적으로 발행되는 과정 로그로 확인 완료했지만, 실질적으로 소비가 첫번째 주문만 되는 현상
     *    - 매수 주문 관련된 데이터가 Redis에 정상적으로 반영된 걸 확인했음에도, 매도 주문 데이터가 계속 반영되지 못하는 현상
     *    - Kafka event 발행 그리고 소비와 관련된 테스트 코드를 작성할 때 고려할 사항이 있을까요??
     *
     *
     *
     */

    @Test
    void testOrderMatchingFlow() throws Exception {
        // Kafka 이벤트가 정상적으로 발행되는지 확인 -> 정상적으로 소비되는지 확인하기
        StockOrderRequestDTO buyTestDTO = new StockOrderRequestDTO(
                samsung.getStockCode(),
                2L,
                1000_000L,
                userA.getId()
        );
        StockOrderRequestDTO sellTestDTO = new StockOrderRequestDTO(
                samsung.getStockCode(),
                2L,
                1000_000L,
                userB.getId()
        );

        // 0. Latch 설정 (StockOrder가 총 2개 발행 → 매수 1, 매도 1)
        OrderPrepareConsumer.latch = new CountDownLatch(2);

        // 1. 매수자 A가 삼성전자 2주 매수 (100만원/주)
        buyStockService.placeSingleBuyOrder(buyTestDTO);

        // 2. 매도자 B의 포트폴리오에 삼성전자 주식 미리 보유
        portfolioStockRepository.save(PortfolioStock.createPortfolioStock(
                sellPortfolio, samsung, new Quantity(2L), new Money(90_000L)
        ));
        // 3. 매도자 B가 삼성전자 2주 매도 (100만원  /주)
        sellStockService.placeSingleSellOrder(sellTestDTO);



        // 4. Kafka 메시지 처리 대기
        boolean completed = OrderPrepareConsumer.latch.await(10, TimeUnit.SECONDS); // milliseconds
        assertTrue(completed, "Kafka 메시지 처리 완료 실패");

        // 5. latch 초기화 (다음 테스트에 영향 방지)
        OrderPrepareConsumer.latch = null;


        // 5. 체결 되는지 확인하기
//        await()
//                .atMost(Duration.ofSeconds(5))
//                .pollInterval(Duration.ofMillis(200))
//                .untilAsserted(() -> {
//                    List<Trade> trades = tradeRepository.findAll();
//                    assertEquals(1, trades.size());
//                });
//
//
//        // 6. 체결 내역 검증
//        List<Trade> trades = tradeRepository.findAll();
//        assertEquals(1, trades.size());
//
//        Trade trade = trades.get(0);
//        assertEquals(1000_000L, trade.getTradePrice().getMoneyValue());
//        assertEquals(2L, trade.getTradeQuantity().getQuantityValue());
//
//        // 7. 포트폴리오 상태 검증
//        PortfolioStock buyerStock = portfolioStockRepository.findByPortfolioAndStock(buyPortfolio, samsung)
//                .orElseThrow(() -> new IllegalStateException("Buyer should have the stock after trade"));
//        assertEquals(2L, buyerStock.getPortfolioQuantity().getQuantityValue());
//
//        boolean sellerStockExists = portfolioStockRepository.findByPortfolioAndStock(sellPortfolio, samsung).isPresent();
//        assertFalse(sellerStockExists, "Seller should have no stock left after full sell");
//
//        // 8. 잔액 검증
//        Portfolio updatedBuyPortfolio = portfolioRepository.findById(buyPortfolio.getId()).orElseThrow();
//        Portfolio updatedSellPortfolio = portfolioRepository.findById(sellPortfolio.getId()).orElseThrow();
//
//        // 매수자는 400만원 중 200만원 사용
//        assertEquals(2000000L, updatedBuyPortfolio.getAvailableCash().getMoneyValue());
//
//        // 매도자는 500만원에 200만원 입금 = 700만원
//        assertEquals(7000000L, updatedSellPortfolio.getAvailableCash().getMoneyValue());
    }

}
