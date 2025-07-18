package com.portfolio2025.first.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 로깅 추가, 발행 실패한 경우에 대한 상황도 고려해서 수정함
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> stringKafkaTemplate; // Match 트리거 역할
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringKafkaTemplate.send("order.created", event.getOrderId().toString(), json).get();
            log.info("[Kafka] order.created 이벤트 발행 성공: orderId={}\n json={}", event.getOrderId(), json);
        } catch (Exception e) {
            log.error("[Kafka] order.created 이벤트 발행 실패: orderId={}, 이유={}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Kafka 발행 실패: orderId = " + event.getOrderId(), e);
        }
    }

    public void publishMatchRequest(String stockCode) {
        try {
            stringKafkaTemplate.send("match.request", stockCode).get();
            log.info("[Kafka] match.request 발행 성공: stockCode={}", stockCode);
        } catch (Exception e) {
            log.error("[Kafka] match.request 발행 실패: stockCode={}, 이유={}", stockCode, e.getMessage(), e);
            throw new RuntimeException("Kafka 발행 실패: stockCode = " + stockCode, e);
        }
    }

}
