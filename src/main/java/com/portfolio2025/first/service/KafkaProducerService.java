package com.portfolio2025.first.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.dto.event.TradeSavedEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka 이벤트 발행을 담당하는 서비스 KafkaProducerService
 *
 * [07.30]
 * (수정) 매수 주문 생성 시 주식의 수량은 따로 검증하지 않아도 되기 때문에 제외함(차라리 제한 로직을 두는게 더 나을 듯 - 최대 100개만)
 *
 *
 * [고민]
 * send().get() -> 동기 처리를 진행하고 있는 상황( 아닌 경우는 어떤게 있고 어떤 차이가 있는지 알고 있어야 함)
 * 발행에 실패한 경우 DLQ 혹은 fallback 로직을 어떻게 구성할지도 생각해보기
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
            stringKafkaTemplate.send("order.created", event.getOrderId().toString(), json).get(); // 동기 처리 중
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
