package com.portfolio2025.first.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.dto.event.TradeSavedEvent;
import com.portfolio2025.first.service.RedisStockOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeSyncConsumer {
    private final ObjectMapper objectMapper;
    private final RedisStockOrderService redisStockOrderService;

    @KafkaListener(
            topics = "trade.synced",
            groupId = "redis-sync",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeTradeSynced(String message) {
        try {
            TradeSavedEvent event = objectMapper.readValue(message, TradeSavedEvent.class);

            // tradeId 로 조회해서 DB에 성공적으로 반영되었는지 확인하기 (정합성) - 비동기로 처리할 때 주의할 점임
            log.info("[Kafka] trade.synced 수신 - tradeId: {}", event.getTradeId());

            if (event.getBuyDTO() != null && event.getBuyDTO().hasQuantity()) {
                redisStockOrderService.pushBuyOrderDTO(event.getBuyDTO());
            }

            if (event.getSellDTO() != null && event.getSellDTO().hasQuantity()) {
                redisStockOrderService.pushSellOrderDTO(event.getSellDTO());
            }

        } catch (Exception e) {
            log.error("[Kafka] trade.synced 처리 실패: {}", e.getMessage(), e);
            // 필요시 DLQ로 전송
        }
    }
}
