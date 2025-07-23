package com.portfolio2025.first.service;

import static jodd.util.ThreadUtil.sleep;

import com.portfolio2025.first.domain.MatchingContext;
import com.portfolio2025.first.domain.MatchingPair;
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
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.repository.PortfolioRepository;
import com.portfolio2025.first.repository.PortfolioStockRepository;
import com.portfolio2025.first.repository.StockOrderRepository;
import com.portfolio2025.first.repository.StockRepository;
import com.portfolio2025.first.repository.TradeRepository;
import com.portfolio2025.first.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * [가정] Springboot 로드 시 DB, Redis 동기화 진행되었다고 가정하고 진행하기
 * 추후에 인자 부분을 DTO 타입으로 선언해서 가지고 오는 방향으로 리팩토링 진행 예정
 * 1. 분산 락 고려할 수 있어야 함(다중 서버 환경에서 Race condition 발생할 수 있음. 동시에 같은 종목 체결 로직을 실행하는 경우)
 * (RedissonClient 활용해서 Lock 획득 - Transaction 진행 - Transaction 올바르게 성공해야 Redis 데이터 반영하기)
 * 2. Redis에 다시 push 해야 하는 상황을 더 고려해보기
 * 3. 다시 push 혹은 실패한 요청 재시도 하는 상황에 대해서 어떻게 처리할지 - idempotency 고려할 수 있어야 함 (완)
 * 4. 무한루프이기 때문에 retry 관련 제한을 반영할 수 있어야 한다 (완) - Controller 기반에서 하는건지 아니면 Service 내에서 진행하면 되는건지??
 * 5. Redis 반영 역시 이벤트 발행으로 - TransactionalListenerEvent(phase = AFTER_COMMIT) 방식 활용 예정
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StockOrderRepository stockOrderRepository;
    private final StockRepository stockRepository;
    private final PortfolioStockRepository portfolioStockRepository; // 포트폴리오 상세 내역
    private final PortfolioRepository portfolioRepository; // 포트폴리오 상위 내역
    private final TradeRepository tradeRepository; // 체결 이력을 담당하는 테이블

    private final RedisStockOrderService redisStockOrderService;
    private final ApplicationEventPublisher eventPublisher;

    // Redisson 분산 락 적용하기
    private final RedissonClient redissonClient;
    // match 실행 하기 전 Lock 획득 여부부터 먼저 확인하기 - 외부에서 먼저 획득한 이후에 Transaction 진행해야 함
    public void matchWithLock(String stockCode) {
        String lockKey = "lock:match:" + stockCode;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            // 3초 동안 시도, 10초 후 자동 해제
            isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("[LOCK] lock 획득 실패 - stockCode: {}", stockCode);
                return;
            }

            log.info("[LOCK] lock 획득 성공 - stockCode: {}", stockCode);
            match(stockCode); // 기존 match 로직 호출

        } catch (InterruptedException e) {
            log.error("[LOCK] 락 획득 중 인터럽트 발생 - {}", e.getMessage(), e);
            Thread.currentThread().interrupt(); // 인터럽트 복구

        } catch (Exception e) {
            log.error("[LOCK] match 실행 중 예외 발생 - {}", e.getMessage(), e);

        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[LOCK] 락 해제 완료 - stockCode: {}", stockCode);
            }
        }
    }

    /** 체결 상황에 대해서 (핵심 로직) **/
    // Redis는 DB에서의 데이터가 온전히 마무리 되었을 때 진행하는 것임
    public void match(String stockCode) {
        final int MAX_RETRY = 10;
        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            // 1. 매칭 후보지 확인하기
            Optional<MatchingPair> pairCandidate = redisStockOrderService.popMatchPair(stockCode);
            if (pairCandidate.isEmpty()) {
                System.out.println("pairCandidate not found..");
                break;
            }

            MatchingPair pair = pairCandidate.get();
            if (pair.isNotPriceMatchable()) {
                redisStockOrderService.pushBack(pair);
                break;
            }

            try {
                matchSinglePair(pair);  // 한 건씩 트랜잭션 처리
            } catch (RetryableMatchException e) {
                retryCount++;
                log.warn("재시도 가능한 예외 발생 ({}회): {}", retryCount, e.getMessage());
                redisStockOrderService.pushBack(pair);
                sleep(100);
            } catch (NonRetryableMatchException e) {
                log.error("재시도 불필요 예외 발생: {}", e.getMessage());
            } catch (Exception e) {
                retryCount++;
                log.error("기타 예외 발생 ({}회): {}", retryCount, e.getMessage(), e);
                redisStockOrderService.pushBack(pair);  // 기본은 재시도 대상으로 처리
                sleep(100);
            }
        }

        if (retryCount >= MAX_RETRY) {
            log.error("[Match] 최대 재시도 초과 - stockCode: {}", stockCode);
            // TODO: 필요시 실패 이벤트 발행 or DLQ
        }

    }

    @Transactional
    private void matchSinglePair(MatchingPair pair) {
        try {
            // 1. 체결 대상 정보 조회 및 검증 -> Idempotency check -> saveTrade() 하는 과정에서 이뤄짐
            MatchingContext context = loadAndValidateEntities(pair);

            // 2. 포트폴리오 및 현금 처리
            processPortfolioUpdates(context);

            // 3. 주문 수량 갱신 (Order도 반영 완료)
            updateOrderStates(context);

            // 4. 체결 이력 저장 -> Trade 관련 UNIQUE 제약 반영 완료 + 이벤트 발행까지
            saveTradeAndPublishEvent(context, pair);

            // 5. Redis 동기화 -> (수정 해보기) Redis 상태 변경을 DB 커밋 후 실행하도록 분리 - TradeRedisSyncListener
//            syncRedisAfterExecution(pair, context.getExecutableQuantity());

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

    private void processPortfolioUpdates(MatchingContext ctx) {
        handleBuyerPortfolioAndCash(ctx.getBuyPortfolio(), ctx.getStock(), ctx.getExecutableQuantity(), ctx.getExecutablePrice());
        handleSellerPortfolioAndCash(ctx.getSellPortfolio(), ctx.getStock(), ctx.getExecutableQuantity(), ctx.getExecutablePrice());
    }

    private void updateOrderStates(MatchingContext ctx) {
        StockOrder buyOrder = ctx.getBuyOrder();
        StockOrder sellOrder = ctx.getSellOrder();

        buyOrder.updateQuantity(ctx.getExecutableQuantity(), ctx.getExecutablePrice());
        sellOrder.updateQuantity(ctx.getExecutableQuantity(), ctx.getExecutablePrice());

        // ✅ 상위 Order 상태도 업데이트
        buyOrder.getOrder().aggregateStatusFromChildren();
        sellOrder.getOrder().aggregateStatusFromChildren();
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

    /** N+1 문제 방지 - JOIN FETCH 방식 적용 **/
    // StockOrderRedisDTO dto, String type - refactoring 진행해주기
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

        // 2. 주문 등록 시점에 예약된 금액 실제 차감 진행
        buyer.releaseAndDeductCash(totalCost);

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

            boolean isEmpty = portfolioStock.decreaseQuantity(quantity); // 내부에서 reserved도 차감되게 변경
            if (isEmpty) {
                portfolioStockRepository.delete(portfolioStock);
            }
        } else {
            throw new IllegalStateException("매도할 주식이 포트폴리오에 없습니다.");
        }
    }
}
