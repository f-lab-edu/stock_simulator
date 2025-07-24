package com.portfolio2025.first.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.dto.event.TradeSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로깅 추가, 발행 실패한 경우에 대한 상황도 고려해서 수정함
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> stringKafkaTemplate; // Match 트리거 역할
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 같은 스레드, 하지만 기존 트랜잭션 중단 후 새로운 트랜잭션 시작
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringKafkaTemplate.send("order.created", event.getOrderId().toString(), json).get(); // Compl..Fu...
            log.info("[Kafka] order.created 이벤트 발행 성공: orderId={}\n json={}", event.getOrderId(), json);
        } catch (Exception e) {
            log.error("[Kafka] order.created 이벤트 발행 실패: orderId={}, 이유={}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Kafka 발행 실패: orderId = " + event.getOrderId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 같은 스레드, 하지만 기존 트랜잭션 중단 후 새로운 트랜잭션 시작
    public void publishMatchRequest(String stockCode) {
        try {
            stringKafkaTemplate.send("match.request", stockCode).get();
            log.info("[Kafka] match.request 발행 성공: stockCode={}", stockCode);
        } catch (Exception e) {
            log.error("[Kafka] match.request 발행 실패: stockCode={}, 이유={}", stockCode, e.getMessage(), e);
            throw new RuntimeException("Kafka 발행 실패: stockCode = " + stockCode, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 같은 스레드, 하지만 기존 트랜잭션 중단 후 새로운 트랜잭션 시작
    public void publishTradeSyncRequest(TradeSavedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringKafkaTemplate.send("trade.synced", event.getTradeId().toString(), json).get();
            log.info("[Kafka] trade.synced 이벤트 발행 성공: tradeId={}\n json={}", event.getTradeId(), json);
        } catch (Exception e) {
            log.error("[Kafka] trade.synced 이벤트 발행 실패: tradeId={}, 이유={}", event.getTradeId(), e.getMessage(), e);
            throw new RuntimeException("Kafka 발행 실패: tradeId = " + event.getTradeId(), e);
        }
    }

}
