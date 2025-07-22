package com.portfolio2025.first.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioType;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.CreateAccountRequestDTO;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.TransferToPortfolioRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.old.BuyStockService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional  // 테스트 후 자동 롤백
@ActiveProfiles("test") // application-test.yml 설정을 적용
class BuyStockServiceIntegrationTest {

    @Autowired private BuyStockService buyStockService;
    @Autowired private AccountService accountService;

    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PortfolioRepository portfolioRepository;

    private User user;
    private Stock samsung;
    private Stock naver;

    @BeforeEach
    void setup() {
        // 유저 생성 및 저장
        user = User.createUser(
                "sunghun",
                "cheonan",
                "010-1234-5678",
                "xxx@gmail",
                "ss"
        );
        userRepository.save(user);

        // 계좌 생성 요청 DTO
        String bankName = "kookmin";
        String accountNumber = "111-222-333";
        String userName = "sunghun";
        CreateAccountRequestDTO createAccountRequestDTO =
                new CreateAccountRequestDTO(bankName, accountNumber, userName);

        // 계좌 생성
        Account account = accountService.createAccount(user.getId(), createAccountRequestDTO);

        // 포트폴리오 생성 및 저장 (이름 대신 타입, 생성일자 지정)
        Portfolio portfolio = Portfolio.createPortfolio(
                user,
                PortfolioType.STOCK,
                LocalDateTime.now()
        );
        portfolioRepository.save(portfolio);

        // 주식 생성 및 저장 (Builder 패턴 사용)
        samsung = Stock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .stockPrice(new Money(100_000L))
                .availableQuantity(new Quantity(1000_000L))
                .build();

        naver = Stock.builder()
                .stockCode("035420")
                .stockName("네이버")
                .stockPrice(new Money(90_000L))
                .availableQuantity(new Quantity(1000_000L))
                .build();

        stockRepository.save(samsung);
        stockRepository.save(naver);
    }

    @Test
    void 여러_주식에_대한_묶음_매수_주문_성공() {
        // given
        User user = userRepository.findByIdForUpdate(1L).orElseThrow();
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(1L).orElseThrow();

        accountService.transferFromAccountToPortfolio(user.getId(),
                new TransferToPortfolioRequestDTO("111-222-333", 1L, 750_000L));

        List<StockOrderRequestDTO> requestList = List.of(
                new StockOrderRequestDTO(samsung.getStockCode(), 5L, 70_000L, this.user.getId()),   // 70,000 * 5 = 350,000
                new StockOrderRequestDTO(naver.getStockCode(), 2L, 200_000L, this.user.getId())       // 200,000 * 2 = 400,000
        );

        // when
        buyStockService.placeBulkBuyOrder(requestList);

        // then
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);

        Order bulkOrder = orders.getFirst();
        assertThat(bulkOrder.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(bulkOrder.getStockOrders()).hasSize(2);

        // 삼성전자 주문 검증
        StockOrder samsungOrder = bulkOrder.getStockOrders().stream()
                .filter(so -> so.getStock().getStockCode().equals("005930"))
                .findFirst().orElseThrow();

        assertThat(samsungOrder.getRequestedQuantity().getQuantityValue()).isEqualTo(5L);
        assertThat(samsungOrder.getRequestedPrice().getMoneyValue()).isEqualTo(350_000L);

        // 네이버 주문 검증
        StockOrder naverOrder = bulkOrder.getStockOrders().stream()
                .filter(so -> so.getStock().getStockCode().equals("035420"))
                .findFirst().orElseThrow();

        assertThat(naverOrder.getRequestedQuantity().getQuantityValue()).isEqualTo(2L);
        assertThat(naverOrder.getRequestedPrice().getMoneyValue()).isEqualTo(400_000L);

        // 총합 가격 검증
        assertThat(bulkOrder.getTotalPrice().getMoneyValue()).isEqualTo(750_000L);
    }
}
