package com.portfolio2025.first.consumer;

import com.portfolio2025.first.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchRequestConsumer {

    private final TradeService tradeService;

    @KafkaListener(topics = "match.request",
            groupId = "trade-match-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeMatchRequest(String stockCode) {
        try {
            log.info("[Kafka] Received match.request for stockCode: {}", stockCode);
            tradeService.matchWithLock(stockCode);

        } catch (Exception e) {
            log.error("[Kafka] Error while processing match.request: stockCode={}, reason={}", stockCode, e.getMessage(), e);
            // 실패한 요청을 DLQ로 보내거나 알림 처리 추가 가능
        }
    }
}
