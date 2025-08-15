package com.portfolio2025.first;

import com.portfolio2025.first.domain.Order;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis에서 중복 처리가 된 것인지 확인하는 RedisRegister
 * 중복으로 처리되지는 않는지를 확인한다.
 * [07.30]
 * (추가)
 *
 * [고민]
 * Redis Register 이라는 클래스명 수정해야 하지 않을까??
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRegister {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String ORDER_PROCESSED_PREFIX = "order:processed:";
    private static final Duration TTL = Duration.ofHours(1); // TTL로 중복 방지 유지 시간 설정

    // 이미 처리된 주문인지 확인
    public boolean isAlreadyProcessed(Order order) {
        String key = ORDER_PROCESSED_PREFIX + order.getId();
        Boolean exists = redisTemplate.hasKey(key);
        log.info("🔍 Redis 중복 처리 확인: {} = {}", key, exists);
        return exists;
    }

    // Redis 최초 등록하기
    public void tryMarkProcessed(Order order) {
        String key = ORDER_PROCESSED_PREFIX + order.getId();
        redisTemplate.opsForValue().setIfAbsent(key, "true", TTL);
    }
}
