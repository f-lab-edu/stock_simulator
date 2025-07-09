package com.portfolio2025.first.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BuyStockServiceIntegrationTest {
    private User user;
    private Account account;
    private Stock stock;
    private StockOrderRequestDTO stockOrderRequestDTO;

    @Autowired
    private BuyStockService buyStockService;
    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private StockOrderRepository stockOrderRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("홍길동")
                .location("서울")
                .phoneNumber("010-1234-5678")
                .email("hong@test.com")
                .userId("user123")
                .build();

        account = Account.builder()
                .accountNumber("111111-11-111111")
                .bankName("국민은행")
                .user(user)
                .userName(user.getName())
                .build();

        stock = Stock.builder()
                .stockCategory(null)
                .stockPrice(new Money(10000L))
                .stockName("삼성전자")
                .stockCode("0000.KR")
                .availableQuantity(new Quantity(1000L))
                .build();

        stockOrderRequestDTO = new StockOrderRequestDTO("0000.KR",
                10L, 10000L, 1L);
    }


    @Test
    void 실제_DB_연관관계_확인() {
        // Repo 에서 각각의 정보를 모두 저장해야 함
        userRepository.save(user);
        accountRepository.save(account);
        stockRepository.save(stock);

        // when (10개 * 10000 원 -> 100000원 매수 주문)
//        accountService.transferFromAccountToPortfolio(account.getAccountNumber(), new Money(1_000_000L));
        buyStockService.placeSingleBuyOrder(stockOrderRequestDTO);

        em.flush();
        em.clear();

        // then
        // 조회하고 -> 각각의 엔티티에서 검증 진행하기
        Order savedOrder = orderRepository.findById(1L).orElseThrow();
        User savedUser = userRepository.findByIdForUpdate(1L).orElseThrow();
        Stock stock = stockRepository.findByStockCode("0000.KR").orElseThrow();

        assertEquals(user.getId(), savedOrder.getUser().getId());  // Order가 유저에 연결됐는지
        assertEquals(this.stock.getId(), savedOrder.getStockOrders().getFirst().getStock().getId());  // Order가 Stock에 연결됐는지
        assertEquals(1, savedOrder.getStockOrders().size());  // Order → StockOrder 연관관계

//        assertEquals(new Money(900_000L), savedUser.getBalance());
        assertEquals(new Quantity(1000L), stock.getAvailableQuantity());
    }
}
