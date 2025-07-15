package com.portfolio2025.first.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import com.portfolio2025.first.repository.OrderRepository;
import com.portfolio2025.first.service.KafkaProducerService;
import com.portfolio2025.first.service.OrderPrepareService;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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

    // ✅ 테스트용 latch (테스트 클래스에서 직접 설정 가능)
    public static CountDownLatch latch;

    @KafkaListener(
            topics = "order.created",
            groupId = "order-prepare-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeOrderCreated(String message) {
        try {
            log.info("[Kafka] Received order.created: {}", message);
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            // 1. Order 조회
            System.out.println("finding stockOrder");
            Order order = orderRepository.findByIdWithStockOrders(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

            System.out.println("order.getStockOrders() = " + order.getStockOrders());


            // 2. 각 StockOrder를 Redis에 등록
            for (StockOrder stockOrder : order.getStockOrders()) {
                try {
                    // Redis 반영 -> Kafka match request (트리거 역할 수행한다)
                    System.out.println("validating and registering to redis...");
                    orderPrepareService.validateAndRegisterToRedis(stockOrder, OrderType.valueOf(event.getOrderType()));
//                    kafkaProducerService.publishMatchRequest(stockOrder.getStock().getStockCode());

                    // ✅ latch countDown: 성공한 경우에만 처리
                    if (latch != null) {
                        latch.countDown();
                    }

                } catch (Exception e) {
                    log.error("[OrderPrepareConsumer] Redis 등록 실패: stockOrderId={}, 이유={}", stockOrder.getId(),
                            e.getMessage());

                    // DLQ or 재처리 어떻게 할지?
                }
            }
        } catch (Exception e) {
            log.error("[Kafka] order.created 메시지 파싱 실패: message={}, 이유={}", message, e.getMessage(), e);
            // TODO: DLQ 또는 에러 처리 로직
        }
    }
}
