package com.portfolio2025.first.integration;


import com.portfolio2025.first.consumer.OrderRequestConsumer;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.User;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.dto.TransferToPortfolioRequestDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.TradeRepository;
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.BuyOrderProcessor;
import com.portfolio2025.first.service.RedisStockOrderService;
import com.portfolio2025.first.service.SellOrderProcessor;
import com.portfolio2025.first.service.StockOrderService;
import com.portfolio2025.first.service.old.BuyStockService;
import com.portfolio2025.first.service.old.SellStockService;
import com.portfolio2025.first.support.IntegrationTestSupport;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


public class TradeIntegrationTest extends IntegrationTestSupport {

    @Autowired private StockOrderService stockOrderService;
    @Autowired private BuyOrderProcessor buyOrderProcessor;
    @Autowired private SellOrderProcessor sellOrderProcessor;
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

    @DynamicPropertySource
    static void overrideKafkaGroupId(DynamicPropertyRegistry r) {
        r.add("spring.kafka.consumer.group-id",
                () -> "order-prepare-group-test-" + UUID.randomUUID());
    }

    @Test
    void 체결_성공() {
        // 매수자 (이체 먼저 진행해야 함) - 200만원 계좌로 부터 인출
        User buyerA = userRepository.findById(1L).orElseThrow();
        accountService.transferFromAccountToPortfolio(buyerA.getId(),
                new TransferToPortfolioRequestDTO("111-222-333333", 1L, 2000_000L));

        // 매도자
        User sellerA = userRepository.findById(2L).orElseThrow();

        // 매도, 매수 주문 생성하기
        stockOrderService.placeSingleOrder(
                new StockOrderRequestDTO("005930", 5L, 100000L, 1L),
                buyOrderProcessor);
        stockOrderService.placeSingleOrder(new StockOrderRequestDTO("005930", 5L, 99000L, 2L),
                sellOrderProcessor);




    }

}
