package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.MatchingPair;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.Trade;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.TradeRepository;
import com.portfolio2025.first.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * [가정] Springboot 로드 시 DB, Redis 동기화 진행되었다고 가정하고 진행하기
 * 추후에 인자 부분을 DTO 타입으로 선언해서 가지고 오는 방향으로 리팩토링 진행 예정
 *
 *
 *
 */

@Service
@RequiredArgsConstructor
public class TradeService {

    private final RedisStockOrderService redisStockOrderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StockOrderRepository stockOrderRepository;
    private final StockRepository stockRepository;
    private final PortfolioStockRepository portfolioStockRepository; // 포트폴리오 상세 내역
    private final PortfolioRepository portfolioRepository; // 포트폴리오 상위 내역
    private final TradeRepository tradeRepository; // 체결 이력을 담당하는 테이블


    /** 매칭 시스템 메인 로직 **/
    @Transactional
    public void match(String stockCode) {

        while (true) {
            // 1. 매칭 후보지 확인하기
            Optional<MatchingPair> pairCandidate = redisStockOrderService.popMatchPair(stockCode);
            if (pairCandidate.isEmpty())
                break;

            MatchingPair pair = pairCandidate.get();
            if (pair.isNotPriceMatchable()) {
                // 다시 넣기
                redisStockOrderService.pushBack(pair);
                break;
            }

            // 체결 수량 그리고 가격 정보
            Quantity executableQuantity = pair.getExecutableQuantity();
            Money executablePrice = new Money(pair.getSellDTO().getRequestedPrice());

            // 2. 실제 DB 엔티티 조회
            StockOrder buyOrder = loadStockOrder(pair.getBuyDTO());
            StockOrder sellOrder = loadStockOrder(pair.getSellDTO());
            Portfolio buyOrderPortfolio = buyOrder.getPortfolio();
            Portfolio sellOrderPortfolio = sellOrder.getPortfolio();
            Stock stock = buyOrder.getStock();

            // 3. 포트폴리오 동기화
            handleBuyerPortfolioAndCash(buyOrderPortfolio, stock, executableQuantity, executablePrice);
            handleSellerPortfolioAndCash(sellOrderPortfolio, stock, executableQuantity, executablePrice);

            // 4. 주문 상태 업데이트
            buyOrder.updateQuantity(executableQuantity, executablePrice);
            sellOrder.updateQuantity(executableQuantity, executablePrice);

            // 5. 체결 로그 생성
            Trade trade = Trade.createTrade(buyOrder, sellOrder, stock, executablePrice, executableQuantity,
                    LocalDateTime.now());
            tradeRepository.save(trade);

            // 6. Redis 동기화
            syncRedisAfterExecution(pair, executableQuantity);
        }
    }

    private void syncRedisAfterExecution(MatchingPair pair, Quantity executableQuantity) {
        // 체결 수량에 따라 기존의 pair 정보를 수정한다
        MatchingPair updatedPair = afterExecution(pair, executableQuantity);
        StockOrderRedisDTO sellDTO = updatedPair.getSellDTO();
        StockOrderRedisDTO buyDTO = updatedPair.getBuyDTO();

        // remainQuantity()가 0보다 큰 경우 반영, 그렇지 않다면 반영하지 않음
        if (sellDTO.hasQuantity()) {
            redisStockOrderService.pushSellOrderDTO(sellDTO);
        }

        if (buyDTO.hasQuantity()) {
            redisStockOrderService.pushBuyOrderDTO(buyDTO);
        }

    }

    private MatchingPair afterExecution(MatchingPair pair, Quantity executableQuantity) {
        return pair.afterExecution(executableQuantity);
    }


    private StockOrder loadStockOrder(StockOrderRedisDTO dto) {
        // stockOrderId로 조회 (JOIN FETCH 방식 적용)
        return stockOrderRepository.findByIdWithAllRelations(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("주문 ID에 해당하는 StockOrder를 찾을 수 없습니다. id="
                        + dto.getId()));
    }

    private void handleBuyerPortfolioAndCash(Portfolio buyer, Stock stock, Quantity quantity,
                                             Money price) {
        // 1. 총 체결 금액 계산 = price * quantity
        Money totalCost = price.multiply(quantity);

        // 2. 주문 등록 시점에 현금 차간하는 방식으로 제외
//        buyer.deductCash(totalCost);

        // 3. 기존 보유 주식 조회
        Optional<PortfolioStock> maybePortfolioStock =
                portfolioStockRepository.findByPortfolioAndStock(buyer, stock);

        if (maybePortfolioStock.isPresent()) {
            // 기존 보유 주식이 있을 경우 수량 + 평균 단가 갱신
            PortfolioStock portfolioStock = maybePortfolioStock.get();
            portfolioStock.addQuantity(quantity, price);
        } else {
            PortfolioStock newStock = PortfolioStock.createPortfolioStock(buyer, stock, quantity, price);
            portfolioStockRepository.save(newStock);
        }
    }

    private void handleSellerPortfolioAndCash(Portfolio seller, Stock stock, Quantity quantity,
                                              Money price) {

        // 1. 총 체결 금액 계산
        Money totalCost = price.multiply(quantity);

        // 2. 현금 증가
        seller.deposit(totalCost);

        // 3. 포트폴리오에서 주식 차감
        Optional<PortfolioStock> maybePortfolioStock =
                portfolioStockRepository.findByPortfolioAndStock(seller, stock);

        if (maybePortfolioStock.isPresent()) {
            PortfolioStock portfolioStock = maybePortfolioStock.get();

            // ❗ 수량 0이면 삭제
            if (portfolioStock.decreaseQuantity(quantity)) {
                portfolioStockRepository.delete(portfolioStock);
            }
        } else {
            // 예외 처리하기
            throw new IllegalStateException("매도할 주식이 포트폴리오에 없습니다.");
        }

    }
}
