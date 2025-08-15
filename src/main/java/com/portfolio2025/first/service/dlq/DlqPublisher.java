package com.portfolio2025.first.service.dlq;

/**
 * DLQ 처리를 담당하는 DlqPublisher
 *
 * [07.30]
 *
 * [고민]
 *
 */
public interface DlqPublisher {
    void publishParseError(String topic, String rawMessage, Exception e); // parsing 과정 중 문제가 발생한 경우
    void publishProcessingError(String topic, String rawMessage, Exception e); // 나머지 처리 중 문제가 발생한 경우
}

