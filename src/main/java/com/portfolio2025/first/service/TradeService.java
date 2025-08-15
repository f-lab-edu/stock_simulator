package com.portfolio2025.first.service;

import static jodd.util.ThreadUtil.sleep;

import com.portfolio2025.first.dto.MatchingPair;
import com.portfolio2025.first.exception.NonRetryableMatchException;
import com.portfolio2025.first.exception.RetryableMatchException;
import com.portfolio2025.first.service.dlq.MatchDlqPublisher;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;


/**
 * [가정] Springboot 로드 시 DB, Redis 동기화 진행되었다고 가정하고 진행하기
 * 추후에 인자 부분을 DTO 타입으로 선언해서 가지고 오는 방향으로 리팩토링 진행 예정
 * 1. 분산 락 고려할 수 있어야 함(다중 서버 환경에서 Race condition 발생할 수 있음. 동시에 같은 종목 체결 로직을 실행하는 경우)
 * (RedissonClient 활용해서 Lock 획득 - Transaction 진행 - Transaction 올바르게 성공해야 Redis 데이터 반영하기)
 * 2. Redis에 다시 push 해야 하는 상황을 더 고려해보기
 * 3. 다시 push 혹은 실패한 요청 재시도 하는 상황에 대해서 어떻게 처리할지 - idempotency 고려할 수 있어야 함 (완)
 * 4. 무한루프이기 때문에 retry 관련 제한을 반영할 수 있어야 한다 (완) - Controller 기반에서 하는건지 아니면 Service 내에서 진행하면 되는건지??
 * 5. Redis 반영 역시 이벤트 발행으로 - TransactionalListenerEvent(phase = AFTER_COMMIT) 방식 활용 예정
 *
 *
 * ----- 수정하기 -----
 *
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final RedisStockOrderService redisStockOrderService;
    private final TradeExecutionService tradeExecutionService;
    private final MatchDlqPublisher matchDlqPublisher;

    private static final int MAX_RETRY = 10;

    public void matchWithRetries(String stockCode) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            Optional<MatchingPair> maybePair = redisStockOrderService.popMatchPair(stockCode);
            if (maybePair.isEmpty()) {
                log.info("❗ 매칭 대상 없음 - stockCode: {}", stockCode);
                break;
            }

            MatchingPair pair = maybePair.get();
            if (pair.isNotPriceMatchable()) {
                redisStockOrderService.pushBack(pair);
                log.info("❗ 가격 조건 불일치 - 재삽입 완료: {}", stockCode);
                break;
            }

            try {
                tradeExecutionService.matchSinglePair(pair);
            } catch (RetryableMatchException e) {
                retryCount = handleRetryable(pair, retryCount, e);
            } catch (NonRetryableMatchException e) {
                handleNonRetryable(pair, e);
                break;
            } catch (Exception e) {
                retryCount = handleUnknown(pair, retryCount, e);
            }
        }

        if (retryCount >= MAX_RETRY) {
            log.error("🚨 최대 재시도 초과 - stockCode: {}", stockCode);
            matchDlqPublisher.publishProcessingError("match.request", formatDlqMessage(stockCode, null), new RuntimeException("MAX_RETRY_EXCEEDED"));
        }
    }

    private int handleRetryable(MatchingPair pair, int retryCount, Exception e) {
        retryCount++;
        log.warn("🔁 Retryable 예외 ({}회): {}", retryCount, e.getMessage());
        redisStockOrderService.pushBack(pair);
        sleep(100);
        return retryCount;
    }

    private void handleNonRetryable(MatchingPair pair, Exception e) {
        log.error("❌ Non-Retryable 예외 발생: {}", e.getMessage());
        matchDlqPublisher.publishProcessingError("match.request", formatDlqMessage(null, pair), e);
    }

    private int handleUnknown(MatchingPair pair, int retryCount, Exception e) {
        retryCount++;
        log.error("❌ 기타 예외 발생 ({}회): {}", retryCount, e.getMessage(), e);
        redisStockOrderService.pushBack(pair);
        sleep(100);
        return retryCount;
    }

    private String formatDlqMessage(String stockCode, MatchingPair pair) {
        return (pair != null) ? pair.toString() : ("stockCode=" + stockCode);
    }
}
