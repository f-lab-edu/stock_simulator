package com.portfolio2025.first.service.dlq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractKafkaDlqPublisher implements DlqPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    protected abstract String getDlqTopic();

    protected void sendToDlq(String errorType, String originalTopic, String rawMessage, Exception e) {
        Map<String, String> payload = Map.of(
                "type", errorType,
                "topic", originalTopic,
                "message", rawMessage,
                "error", e.getMessage()
        );

        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(getDlqTopic(), json);
            log.warn("📦 DLQ 전송 완료: {}", json);
        } catch (JsonProcessingException ex) {
            log.error("❌ DLQ 직렬화 실패", ex);
        } catch (Exception ex) {
            log.error("❌ DLQ 전송 실패", ex);
        }
    }

    @Override
    public void publishParseError(String topic, String rawMessage, Exception e) {
        sendToDlq("parse_error", topic, rawMessage, e);
    }

    @Override
    public void publishProcessingError(String topic, String rawMessage, Exception e) {
        sendToDlq("processing_error", topic, rawMessage, e);
    }
}

