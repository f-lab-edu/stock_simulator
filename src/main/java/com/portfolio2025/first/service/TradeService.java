package com.portfolio2025.first.service;

import static jodd.util.ThreadUtil.sleep;

import com.portfolio2025.first.domain.MatchingPair;
import com.portfolio2025.first.exception.NonRetryableMatchException;
import com.portfolio2025.first.exception.RetryableMatchException;
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
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final RedisStockOrderService redisStockOrderService;
    private final TradeExecutionService tradeExecutionService;

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
                tradeExecutionService.matchSinglePair(pair);  // 한 건씩 트랜잭션 처리 -> AOP에 맞게 외부 Service 등록
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

}
