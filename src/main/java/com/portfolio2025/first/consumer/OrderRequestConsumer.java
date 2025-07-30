package com.portfolio2025.first.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.OrderValidator;
import com.portfolio2025.first.RedisRegister;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.service.KafkaDlqService;
import com.portfolio2025.first.service.KafkaProducerService;
import com.portfolio2025.first.service.RedisStockOrderService;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 이벤트 소비를 담당하는 OrderRequestConsumer
 *
 * [07.30]
 * (추가) publishInvalidMessage - 역직렬화 실패를 대비한 재처리 담당한 메서드 호출
 * (추가) initStrategyMap - 초기화 전 미리 주입하면 의존성 문제 발생으로 PostConstruct 활용..
 * [고민]
 * 재시도 + DLQ + Idempotency 방지하는 설계로 진행하기
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRequestConsumer {

    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;
    private final OrderValidator orderValidator;
    private final RedisRegister redisRegister;
    private final KafkaDlqService kafkaDlqService;
    private final RedisStockOrderService redisStockOrderService;

    private Map<OrderType, Consumer<StockOrder>> redisPushStrategy;

    @PostConstruct
    public void initStrategyMap() {
        redisPushStrategy = Map.of(
                OrderType.BUY, redisStockOrderService::pushBuyOrder,
                OrderType.SELL, redisStockOrderService::pushSellOrder
        );
    }

    @KafkaListener(
            topics = "order.created",
            groupId = "order-prepare-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeOrderCreated(String message, Acknowledgment ack) throws InterruptedException {
        log.info("🟢 Kafka Received: {}", message);

        OrderCreatedEvent event = parseEvent(message, ack);
        if (event == null) return;

        Order order = fetchAndValidateOrder(event, message, ack);
        if (order == null) return;

        try {
            processOrder(order, event);
            redisRegister.markProcessed(order);
        } catch (Exception e) {
            kafkaDlqService.sendProcessingError("order.created", message, e);
            log.error("❌ 주문 처리 중 예외 발생 → DLQ 전송", e);
        } finally {
            ack.acknowledge();
        }
    }

    private OrderCreatedEvent parseEvent(String message, Acknowledgment ack) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("🟢 Parsed OrderCreatedEvent: {}", event);
            return event;
        } catch (JsonProcessingException e) {
            kafkaDlqService.sendParseError("order.created", message, e);
            log.error("❌ JSON 파싱 실패 → DLQ 전송", e);
            ack.acknowledge();
            return null;
        }
    }

    private Order fetchAndValidateOrder(OrderCreatedEvent event, String message, Acknowledgment ack) {
        try {
            Order order = orderValidator.findOrderWithRetry(event.getOrderId());
            if (redisRegister.isAlreadyProcessed(order)) {
                log.warn("🔁 Already processed orderId={}", order.getId());
                ack.acknowledge();
                return null;
            }
            return order;
        } catch (Exception e) {
            kafkaDlqService.sendProcessingError("order.created", message, e);
            log.error("❌ 주문 조회 실패 → DLQ 전송", e);
            ack.acknowledge();
            return null;
        }
    }

    private void processOrder(Order order, OrderCreatedEvent event) {
        OrderType orderType = OrderType.valueOf(event.getOrderType());
        Consumer<StockOrder> redisPusher = redisPushStrategy.get(orderType);

        if (redisPusher == null) {
            throw new IllegalArgumentException("❌ 지원하지 않는 주문 타입: " + orderType);
        }

        for (StockOrder stockOrder : order.getStockOrders()) {
            orderValidator.validate(stockOrder);
            redisPusher.accept(stockOrder);
            kafkaProducerService.publishMatchRequest(event.getStockCode());
        }
    }
}
