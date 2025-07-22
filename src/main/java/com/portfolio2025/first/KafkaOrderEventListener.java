package com.portfolio2025.first;


import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderEventListener {

    private final KafkaProducerService kafkaProducerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("[KafkaOrderEventListener] 트랜잭션 커밋 후 Kafka 발행: {}", event);
        try {
            kafkaProducerService.publishOrderCreated(event);
            log.info("✅ Kafka publish success: {}", event);
        } catch (Exception e) {
            log.error("❌ Kafka publish failed for event: {}", event, e);
            // 테스트 중이면 재시도나 fallback 전략을 여기에 넣을 수도 있음

        }
    }
}
