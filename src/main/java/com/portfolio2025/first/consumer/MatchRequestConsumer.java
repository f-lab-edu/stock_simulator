package com.portfolio2025.first.consumer;

import com.portfolio2025.first.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 매칭 체결을 요구하는 이벤트를 소비하는 MatchRequestConsumer
 *
 * [07.31]
 * (삭제) Exception 발생 시에는 offset 커밋하지 않는다 -> DLQ 처리하기
 * (수정) matchWithLock -> 분산 락 없이 진행해도 가능함 -> matchWithRetries로 수정함
 * [고민]
 * 재시도 + DLQ + Idempotency 방지하는 설계로 진행하기
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchRequestConsumer {

    private final TradeService tradeService;

    @KafkaListener(
            topics = "match.request",
            groupId = "trade-match-group"
    )
    public void consumeMatchRequest(String stockCode, Acknowledgment ack) {
        try {
            // 주문 생성 - Redis 반영 - Match.request 이벤트 발행 - 소비하는 과정 중임
            // MatchRequestConsumer - OrderRequestConsumer로 이어지는 상황
            log.info("[Kafka] Received match.request for stockCode: {}", stockCode);
            // 수십초 이내에 엄청나게 많은 매칭 체결 요청을 받은 상황이라면 어떻게 해결할 수 있을까??
            tradeService.matchWithRetries(stockCode);

        } catch (Exception e) {
            ack.acknowledge();
            log.error("[Kafka] Error while processing match.request: stockCode={}, reason={}", stockCode, e.getMessage(), e);
            // 실패한 요청을 DLQ로 보내거나 알림 처리 추가 가능
        } finally {
            ack.acknowledge();
            log.info("✅ Kafka offset manually committed");
        }
    }
}
