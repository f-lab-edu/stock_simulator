package com.portfolio2025.first.config;


import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Kafka 이벤트 발행 관련 Config KafkaProducerConfig
 *
 * [07.30]
 * (추가) "invalid.order.created" 토픽 추가
 *
 * [고민]
 *
 *
 */

@Configuration
@Profile({"dev","test"}) // 운영 분리 원하면
public class KafkaTopicConfig {
    @Bean public NewTopic matchRequestTopic()   { return TopicBuilder.name("match.request").partitions(2).replicas(1).build(); }
    @Bean public NewTopic orderCreatedTopic()   { return TopicBuilder.name("order.created").partitions(2).replicas(1).build(); }
    @Bean public NewTopic tradeSyncedTopic()    { return TopicBuilder.name("trade.synced").partitions(2).replicas(1).build(); }
    @Bean public NewTopic invalidOrderCreatedTopic() {
        return TopicBuilder.name("invalid.order.created").partitions(2).replicas(1).build();
    }
}
