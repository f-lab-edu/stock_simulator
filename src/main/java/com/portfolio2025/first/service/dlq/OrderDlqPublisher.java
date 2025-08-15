package com.portfolio2025.first.service.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 관련 DLQ 처리를 담당하는 OrderDlqPublisher
 *
 * [07.30]
 *
 * [고민]
 *
 */
@Component
public class OrderDlqPublisher extends AbstractKafkaDlqPublisher {

    public OrderDlqPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    @Override
    protected String getDlqTopic() {
        return "dlq.order.created";
    }
}

