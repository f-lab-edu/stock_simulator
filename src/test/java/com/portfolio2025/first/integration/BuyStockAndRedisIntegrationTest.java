package com.portfolio2025.first.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.CreateAccountRequestDTO;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.TransferToPortfolioRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.old.BuyStockService;
import com.portfolio2025.first.service.RedisStockOrderService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BuyStockAndRedisIntegrationTest {

    @Autowired private BuyStockService buyStockService;
    @Autowired private RedisStockOrderService redisStockOrderService;
    @Autowired private StockRepository stockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountService accountService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    private User user;
    private Stock samsung;
    private Account account;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.createUser("test", "seoul", "010-0000-0000", "test@email.com", "pw"));

        samsung = stockRepository.save(Stock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .stockPrice(new Money(100_000L)) // 10만원 짜리
                .availableQuantity(new Quantity(1_000_000L)) // 100만 개 수량 가지고 있는 상황
                .build());

        CreateAccountRequestDTO createAccountRequestDTO =
                new CreateAccountRequestDTO("kookmin", "111-222-333", "sunghun");
        account = accountService.createAccount(user.getId(), createAccountRequestDTO);


        // 유저에게 포트폴리오 생성 (테스트 유틸 메서드로 추가해도 됨)
        portfolio = Portfolio.createPortfolio(user, PortfolioType.STOCK, LocalDateTime.now());
        portfolioRepository.save(portfolio);

        // 계좌에서 400만원 인출, 투자금 400만원인 상황
        accountService.transferFromAccountToPortfolio(user.getId(), new TransferToPortfolioRequestDTO(
                "111-222-333",
                portfolio.getId(),
                4000_000L));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("BUY_QUEUE_005930");
    }

    @Test
    void 단건_매수_주문이_DB에_저장되고_Redis에도_등록된다() {
        // Given: 매수 주문 요청 DTO
        User found = userRepository.findByIdForUpdate(this.user.getId()).orElseThrow();

        StockOrderRequestDTO requestDTO = new StockOrderRequestDTO(
                samsung.getStockCode(), // "005930"
                3L,                     // 수량
                95000L,                 // 단가
                found.getId()
        );

        // When: 매수 주문 실행
        buyStockService.placeSingleBuyOrder(requestDTO); // Order - stockOrder 같이 저장된 상황
        Optional<Order> order = orderRepository.findById(1L);

        redisStockOrderService.pushBuyOrderDTO(
                StockOrderRedisDTO.from(order.get().getStockOrders().getFirst()));

        // Then: Redis에 값이 잘 들어갔는지 확인
        Long redisCount = redisTemplate.opsForZSet().size("BUY_QUEUE_005930");
        assertThat(redisCount).isEqualTo(1);

        // Redis에서 pop 해서 DTO 확인
        Optional<StockOrderRedisDTO> maybeDTO = redisStockOrderService.popBestBuyOrder("005930");
        assertThat(maybeDTO).isPresent();
        StockOrderRedisDTO dto = maybeDTO.get();

        assertThat(dto.getStockCode()).isEqualTo("005930");
        assertThat(dto.getRemainQuantity()).isEqualTo(3L);
        assertThat(dto.getRequestedPrice()).isEqualTo(95000L);
        assertThat(dto.getId()).isEqualTo(this.user.getId());

        assertThat(order.get().getTotalPrice()).isEqualTo(new Money(3L * 95000L));
    }

    @Test
    void bulk매수주문_DB_Redis_확인() {

    }

    @Test
    void 오류확인_매수시_현금부족() {

    }

}
