package com.portfolio2025.first.consumer.dlq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 관련 이벤트 처리 DLQ 담당하는 InvalidOrderConsumer
 *
 * [07.30]
 * (추가) consumeInvalidOrder() 메서드 추가 (새로운 리스너 메서드 추가함)
 *
 * [고민]
 *
 */
@Component
@Slf4j
public class InvalidOrderConsumer {

    @KafkaListener(
            topics = "invalid.order.created",
            groupId = "invalid-order-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeInvalidOrder(String message) {
        log.warn("⚠️ Received message from DLQ: {}", message);

        // 선택적으로 DB 저장, 관리자 알림 등..
    }
}
