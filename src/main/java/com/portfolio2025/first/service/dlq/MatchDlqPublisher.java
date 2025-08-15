package com.portfolio2025.first.service.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Match 체결 관련 DLQ 처리를 담당하는 MatchDlqPublisher
 *
 * [07.30]
 *
 * [고민]
 *
 */
@Component
public class MatchDlqPublisher extends AbstractKafkaDlqPublisher {

    public MatchDlqPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    @Override
    protected String getDlqTopic() {
        return "dlq.match.failed";
    }
}

