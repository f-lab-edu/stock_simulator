package com.portfolio2025.first.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio2025.first.domain.Account;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockCategory;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.repository.AccountRepository;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.old.BuyStockService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuyStockServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private StockOrderRepository stockOrderRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BuyStockService buyStockService;
    @InjectMocks
    private AccountService accountService;


    private User user;
    private Account account;
    private Stock stock;
    private StockOrderRequestDTO stockOrderRequestDTO;
    @Mock
    private StockCategory stockCategory;

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
                .stockCategory(stockCategory)
                .stockPrice(new Money(10000L))
                .stockName("삼성전자")
                .stockCode("0000.KR")
                .availableQuantity(new Quantity(1000L))
                .build();

        stockOrderRequestDTO = new StockOrderRequestDTO("0000.KR",
                10L, 10000L, 1L);
    }

    // 비즈니스 코드 꼼꼼하게 체크해야 하는 상황
    // 단순한 조회는.
    // 같은 수준의 객체는 한 번만 해도 됨.
    @Test
    void 매수_주문_성공() {
        // when
        when(userRepository.findByIdForUpdate(any(Long.class))).thenReturn(Optional.ofNullable(user));
        when(stockRepository.findByStockCode(any(String.class))).thenReturn(Optional.ofNullable(stock));
        when(accountRepository.findByAccountNumberWithLock(account.getAccountNumber())).thenReturn(
                Optional.ofNullable(account));

        // then
//        accountService.transferFromAccountToPortfolio(account.getAccountNumber(), new Money(1_000_000L));
        buyStockService.placeSingleBuyOrder(stockOrderRequestDTO);

        // given (DB 없이 진행해서 verify 검증 진행해야 함)
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertEquals(user, savedOrder.getUser());
        assertEquals(new Money(100_000L), savedOrder.getTotalPrice());
//        assertEquals(new Money(900_000L), user.getBalance());
        assertEquals(new Money(9_000_000L), account.getAvailableCash());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(stockOrderRepository, times(1)).save(any(StockOrder.class), any(Order.class));
    }

    @Test
    void 매수_주문_가격부족_실패() {

    }

    @Test
    void 매수_주문_수량부족_실패() {

    }


}
