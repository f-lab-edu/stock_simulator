package com.portfolio2025.first.controller;

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
import com.portfolio2025.first.repository.UserRepository;
import com.portfolio2025.first.service.AccountService;
import com.portfolio2025.first.service.BuyOrderProcessor;
import com.portfolio2025.first.service.SellOrderProcessor;
import com.portfolio2025.first.service.StockOrderService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-order")
@RequiredArgsConstructor
public class TestOrderController {

    private final StockOrderService stockOrderService;
    private final BuyOrderProcessor buyOrderProcessor;
    private final SellOrderProcessor sellOrderProcessor;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioStockRepository portfolioStockRepository;
    private final StockRepository stockRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private User userA;
    private User userB;
    private Stock samsung;

    private Portfolio buyPortfolio;
    private Portfolio sellPortfolio;

    private boolean initialized = false;

    private void initIfNecessary() {
        if (initialized) return;

        // 매수자 A
        userA = userRepository.save(User.createUser("sunghun", "suwon", "010-1234-5678", "test@email.com", "pw"));
        CreateAccountRequestDTO dtoA = new CreateAccountRequestDTO("kookmin", "111111-22-333333", "sunghun");
        accountService.createAccount(userA.getId(), dtoA);
        portfolioRepository.save(Portfolio.createPortfolio(userA, PortfolioType.STOCK, LocalDateTime.now()));
        buyPortfolio = portfolioRepository.findByUserIdAndPortfolioType(userA.getId(), PortfolioType.STOCK).orElseThrow();
        accountService.transferFromAccountToPortfolio(userA.getId(),
                new TransferToPortfolioRequestDTO("111111-22-333333", buyPortfolio.getId(), 4_000_000L));

        // 매도자 B
        userB = userRepository.save(User.createUser("sungsung", "seoul", "010-5678-0000", "test@naver.com", "pw12345"));
        CreateAccountRequestDTO dtoB = new CreateAccountRequestDTO("sinhan", "444444-55-666666", "sungsung");
        accountService.createAccount(userB.getId(), dtoB);
        portfolioRepository.save(Portfolio.createPortfolio(userB, PortfolioType.STOCK, LocalDateTime.now()));
        sellPortfolio = portfolioRepository.findByUserIdAndPortfolioType(userB.getId(), PortfolioType.STOCK).orElseThrow();
        accountService.transferFromAccountToPortfolio(userB.getId(),
                new TransferToPortfolioRequestDTO("444444-55-666666", sellPortfolio.getId(), 5_000_000L));

        // 주식
        samsung = stockRepository.save(Stock.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .stockPrice(new Money(100_000L))
                .availableQuantity(new Quantity(1_000_000L))
                .build());

        // 매도자 B가 2주 보유
        portfolioStockRepository.save(PortfolioStock.createPortfolioStock(
                sellPortfolio, samsung, new Quantity(2L), new Money(90_000L)
        ));

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();



        initialized = true;
    }

    @PostMapping("/buy")
    @Transactional
    public String testBuyOrder() {
        initIfNecessary(); // 최초 한 번만 실행


        // 예시 DTO 생성 (매수자 A)
        StockOrderRequestDTO buyTestDTO = new StockOrderRequestDTO(
                samsung.getStockCode(),
                2L,
                1000_000L,
                userA.getId()
        );

        stockOrderService.placeSingleOrder(buyTestDTO, buyOrderProcessor);
        return "Buy order placed";
    }

    @PostMapping("/sell")
    public String testSellOrder() {
        if (!initialized) return "❌ Please call /buy first to initialize test data.";
        // 예시 DTO 생성 (매도자 B)
        StockOrderRequestDTO sellTestDTO = new StockOrderRequestDTO(
                samsung.getStockCode(),
                2L,
                1000_000L,
                userB.getId()
        );


        stockOrderService.placeSingleOrder(sellTestDTO, sellOrderProcessor);
        return "Sell order placed";
    }
}

