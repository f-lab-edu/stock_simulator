package com.portfolio2025.first.listener;


import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매수 혹은 매도 주문 생성 이후 REDIS에 반영 위한 event 발행
 * 후속 처리 가능함
 * Transaction commit 이후 동기적으로 이벤트 발행 처리하기 - AFTER_COMMIT
 */


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRedisSyncListener {

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
