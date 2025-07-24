package com.portfolio2025.first.kafka;

import com.portfolio2025.first.consumer.OrderRequestConsumer;
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
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.TradeRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.BuyOrderProcessor;
import com.portfolio2025.first.service.SellOrderProcessor;
import com.portfolio2025.first.service.StockOrderService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { // 이 부분 확인해볼 것....
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
@Rollback(value = false)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmbeddedKafkaTest {

    @Autowired private OrderRequestConsumer orderRequestConsumer;
    @Autowired private StringRedisTemplate redisTemplate;


    @Autowired private StockRepository stockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountService accountService;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private PortfolioStockRepository portfolioStockRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private StockOrderService stockOrderService;
    @Autowired private BuyOrderProcessor buyOrderProcessor;
    @Autowired private SellOrderProcessor sellOrderProcessor;



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

        // 연결된 Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // 매도자 B의 포트폴리오에 삼성전자 주식 미리 보유
        portfolioStockRepository.save(PortfolioStock.createPortfolioStock(
                sellPortfolio, samsung, new Quantity(2L), new Money(90_000L)
        ));
        portfolioRepository.flush();

    }


    @Test
    @Commit
    void testKafkaOrderCreatedEventConsumption() throws Exception {
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


        // 1. 매수자 A가 삼성전자 2주 매수 (100만원/주)
        stockOrderService.placeSingleOrder(buyTestDTO, buyOrderProcessor);
        // 3. 매도자 B가 삼성전자 2주 매도 (100만원  /주)
        stockOrderService.placeSingleOrder(sellTestDTO, sellOrderProcessor);


        // 체결 검증은 아래와 같음
//        List<Trade> trades = tradeRepository.findAll();
//        assertEquals(1, trades.size());
//
//        Trade trade = trades.get(0);
//        assertEquals(1000_000L, trade.getTradePrice().getMoneyValue());
//        assertEquals(2L, trade.getTradeQuantity().getQuantityValue());
//
//        // 7. 포트폴리오 상태 검증은
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
//        assertEquals(7000000L, updatedSellPortfolio.getAvailableCash().getMoneyValue())


    }
}
