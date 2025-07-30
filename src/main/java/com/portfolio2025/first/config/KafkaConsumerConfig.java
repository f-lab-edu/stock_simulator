package com.portfolio2025.first.config;

import com.portfolio2025.first.dto.event.OrderCreatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Kafka 이벤트 발행 관련 Config KafkaProducerConfig
 *
 * [07.30]
 * (추가) "invalid.order.created" 토픽 추가
 *
 * [고민]
 * send().get() -> 동기 처리를 진행하고 있는 상황( 아닌 경우는 어떤게 있고 어떤 차이가 있는지 알고 있어야 함)
 * 발행에 실패한 경우 DLQ 혹은 fallback 로직을 어떻게 구성할지도 생각해보기
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-prepare-group"); // 그룹ID 동일 유지 가능
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋 설정하기 위함
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.MANUAL); // 수동 커밋으로 설정하기 위함
        return factory;
    }

}

