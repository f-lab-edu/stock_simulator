package com.portfolio2025.first.config;


import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
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
public class KafkaProducerConfig {

    @Bean
    public NewTopic matchRequestTopic() {
        // match.request 토픽 발행해두기
        return TopicBuilder.name("match.request")
                .partitions(2)
                .replicas(1)  // 운영 시 2 이상 권장
                .build();
    }

    // 주문 생성 토픽
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
                .partitions(2)
                .replicas(1)
                .build();
    }

    // 체결 Sync
    @Bean
    public NewTopic tradeSyncedTopic() {
        return TopicBuilder.name("trade.synced")
                .partitions(2)
                .replicas(1)
                .build();
    }

    // 주문 생성 DQL 담당하는 토픽
    @Bean
    public NewTopic invalidOrderCreatedTopic() {
        return TopicBuilder.name("invalid.order.created")
                .partitions(2)       // 실패 메시지니까 1개로도 충분 (필요시 확장 가능)
                .replicas(1)         // 운영에선 2~3 추천
                .build();
    }

    // String 전용 KafkaTemplate
    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 핵심!
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }
}
