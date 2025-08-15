package com.portfolio2025.first.service;

import com.portfolio2025.first.dto.MatchingContext;
import com.portfolio2025.first.dto.MatchingPair;
import com.portfolio2025.first.domain.Portfolio;
import com.portfolio2025.first.domain.PortfolioStock;
import com.portfolio2025.first.domain.Trade;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import com.portfolio2025.first.dto.event.TradeSavedEvent;
import com.portfolio2025.first.exception.NonRetryableMatchException;
import com.portfolio2025.first.exception.RetryableMatchException;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.TradeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisConnectionException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeExecutionService {

    private final TradeRepository tradeRepository;
    private final PortfolioStockRepository portfolioStockRepository;
    private final StockOrderRepository stockOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void matchSinglePair(MatchingPair pair) {
        try {
            // 1. 체결 대상 정보 조회 및 검증 -> Idempotency check -> saveTrade() 하는 과정에서 이뤄짐
            MatchingContext context = loadAndValidateEntities(pair);

            // 2. 포트폴리오 및 현금 처리
            processPortfolioUpdates(context);

            // 3. 주문 수량 갱신 (Order도 반영 완료)
            updateOrderStates(context);

            // 4. 체결 이력 저장 -> Trade 관련 UNIQUE 제약 반영 완료 + 이벤트 발행까지
            saveTradeAndPublishEvent(context, pair);

        } catch (EntityNotFoundException | IllegalStateException e) {
            throw new NonRetryableMatchException(e.getMessage()); // 구조적 문제
        } catch (RedisConnectionException | DataAccessException e) {
            throw new RetryableMatchException(e.getMessage()); // 일시적 장애
        }
    }

    private MatchingContext loadAndValidateEntities(MatchingPair pair) {
        Quantity quantity = pair.getExecutableQuantity();
        Money price = new Money(pair.getSellDTO().getRequestedPrice());

        StockOrder buyOrder = loadStockOrder(pair.getBuyDTO());
        StockOrder sellOrder = loadStockOrder(pair.getSellDTO());

        Portfolio buyPortfolio = buyOrder.getPortfolio();
        Portfolio sellPortfolio = sellOrder.getPortfolio();
        Stock stock = buyOrder.getStock();

        return new MatchingContext(buyOrder, sellOrder, buyPortfolio, sellPortfolio, stock, quantity, price);
    }

    /** N+1 문제 방지 - JOIN FETCH 방식 적용 **/
    // StockOrderRedisDTO dto, String type - refactoring 진행해주기
    private StockOrder loadStockOrder(StockOrderRedisDTO dto) {
        // stockOrderId로 조회 (JOIN FETCH 방식 적용)
        return stockOrderRepository.findByIdWithAllRelations(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("주문 ID에 해당하는 StockOrder를 찾을 수 없습니다. id="
                        + dto.getId()));
    }

    private void processPortfolioUpdates(MatchingContext ctx) {
        handleBuyerPortfolioAndCash(ctx.getBuyPortfolio(), ctx.getStock(), ctx.getExecutableQuantity(), ctx.getExecutablePrice());
        handleSellerPortfolioAndCash(ctx.getSellPortfolio(), ctx.getStock(), ctx.getExecutableQuantity(), ctx.getExecutablePrice());
    }

    private void updateOrderStates(MatchingContext ctx) {
        StockOrder buyOrder = ctx.getBuyOrder();
        StockOrder sellOrder = ctx.getSellOrder();

        buyOrder.applyExecution(ctx.getExecutableQuantity(), ctx.getExecutablePrice());
        sellOrder.applyExecution(ctx.getExecutableQuantity(), ctx.getExecutablePrice());

        // ✅ 상위 Order 상태도 업데이트
        buyOrder.getOrder().aggregateStatusFromStockOrders();
        sellOrder.getOrder().aggregateStatusFromStockOrders();
    }

    private void saveTradeAndPublishEvent(MatchingContext ctx, MatchingPair pair) {
        Long buyOrderId = ctx.getBuyOrder().getId();
        Long sellOrderId = ctx.getSellOrder().getId();

        // 멱등성 체크
        if (tradeRepository.existsByBuyOrderAndSellOrder(buyOrderId, sellOrderId)) {
            log.warn("[Trade] 이미 체결된 거래입니다. 저장을 생략합니다. buyOrderId={}, sellOrderId={}", buyOrderId, sellOrderId);
            return;
        }

        Trade trade = Trade.createTrade(
                ctx.getBuyOrder(),
                ctx.getSellOrder(),
                ctx.getStock(),
                ctx.getExecutablePrice(),
                ctx.getExecutableQuantity(),
                LocalDateTime.now());
        tradeRepository.save(trade);

        // 🎯 이벤트 발행 분리
        publishTradeSavedEvent(trade.getId(), pair, ctx.getExecutableQuantity());
    }

    private void publishTradeSavedEvent(Long tradeId, MatchingPair pair, Quantity executedQuantity) {
        MatchingPair updatedPair = afterExecution(pair, executedQuantity);

        TradeSavedEvent event = new TradeSavedEvent(
                tradeId,
                updatedPair.getBuyDTO(),
                updatedPair.getSellDTO()
        );

        eventPublisher.publishEvent(event);
    }

    private MatchingPair afterExecution(MatchingPair pair, Quantity executableQuantity) {
        return pair.afterExecution(executableQuantity);
    }

    private void handleBuyerPortfolioAndCash(Portfolio buyer, Stock stock, Quantity quantity,
                                             Money price) {
        // 1. 총 체결 금액 계산 = price * quantity
        Money totalCost = price.multiply(quantity);

        // 2. 주문 등록 시점에 예약된 금액 실제 차감 진행 (차감을 미리 매수 주문 넣을 때 진행했기에 차감은 진행하지 않음)
        buyer.cancelReservedCash(totalCost);

        // 3. 기존 보유 주식 조회
        Optional<PortfolioStock> maybePortfolioStock =
                portfolioStockRepository.findByPortfolioAndStockWithLock(buyer, stock);

        if (maybePortfolioStock.isPresent()) {
            // 기존 보유 주식이 있을 경우 수량 + 평균 단가 갱신
            PortfolioStock portfolioStock = maybePortfolioStock.get();
            portfolioStock.applyBuy(quantity, price);
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
                portfolioStockRepository.findByPortfolioAndStockWithLock(seller, stock);

        if (maybePortfolioStock.isPresent()) {
            PortfolioStock portfolioStock = maybePortfolioStock.get();

            boolean isEmpty = portfolioStock.decreaseQuantity(quantity); // 내부에서 reserved도 차감되게 변경
            if (isEmpty) {
                portfolioStockRepository.delete(portfolioStock);
            }
        } else {
            throw new IllegalStateException("매도할 주식이 포트폴리오에 없습니다.");
        }
    }

}
