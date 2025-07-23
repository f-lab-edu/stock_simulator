package com.portfolio2025.first.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.service.KafkaProducerService;
import com.portfolio2025.first.service.OrderPrepareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 매도, 매수 주문 Topic 구독
 * Order 검증 진행하기 -> Redis 반영
 *
 */


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPrepareConsumer {

    private final OrderRepository orderRepository;
    private final OrderPrepareService orderPrepareService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;


    @KafkaListener(
            topics = "order.created",
            groupId = "order-prepare-group",
            concurrency ="2",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeOrderCreated(String message, Acknowledgment ack) throws InterruptedException {
        log.info("🟢 Kafka Received: {}", message);

        OrderCreatedEvent event = null;
        try {
            event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("🟢 Parsed OrderCreatedEvent: {}", event);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse JSON", e);
            return;
        }

        Order order = null;
        int retry = 0;
        while (retry < 5) {
            order = orderRepository.findByIdWithStockOrders(event.getOrderId()).orElse(null);
            log.info("🔁 Try {}: order = {}", retry, order);
            if (order != null)
                break;
            Thread.sleep(200);
            retry++;
        }

        if (order == null) {
            log.error("❌ Order not found in DB even after retries: orderId={}", event.getOrderId());
            return;
        }

        if (order.getStockOrders() == null) {
            log.error("❌ StockOrders is NULL");
            return;
        }

        if (order.getStockOrders().isEmpty()) {
            log.error("❌ StockOrders is EMPTY");
            return;
        }

        try {
            for (StockOrder stockOrder : order.getStockOrders()) {
                log.info("🟢 Validating stockOrder: {}", stockOrder.getId());
                // Redis에 반영 (주문 하나라도 반영되는 순간 Kafka 이벤트 요청해서 match를 요청함)
                orderPrepareService.validateAndRegisterToRedis(stockOrder, OrderType.valueOf(event.getOrderType()));
                // 체결을 위한 Kafka 이벤트 요청
                kafkaProducerService.publishMatchRequest(stockOrder.getStock().getStockCode());
            }


        } catch (Exception e) {
            // ❌ 커밋하지 않음 → 재처리 대상
            log.error("❌ Error while processing stockOrders: {}", e.getMessage());
            // 재발행하는 경우 Idempotency는 어떻게 구성할지 생각할 수 있어야 함.
        } finally {
            // ✅ 모든 처리 완료 후 커밋
            ack.acknowledge();
            log.info("✅ Kafka offset manually committed");
        }
    }
}
