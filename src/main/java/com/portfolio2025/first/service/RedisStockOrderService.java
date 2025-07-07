package com.portfolio2025.first.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.converter.StockOrderRedisConverter;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 매수 주문 시 가격(음수화) - 시간 (pop 진행 시 reverse)
 * 매도 주문 시 가격(양수화) + 시간으로
 *
 */


@Service
@RequiredArgsConstructor
public class RedisStockOrderService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StockOrderRedisConverter converter;

    private String getBuyKey(String stockCode) {
        return "BUY_QUEUE_" + stockCode;
    }

    private String getSellKey(String stockCode) {
        return "SELL_QUEUE_" + stockCode;
    }

    /** Redis에 매수 주문 저장 (SortedSet - ZADD) **/
    public void pushBuyOrder(StockOrder order) {
        StockOrderRedisDTO dto = converter.toDTO(order);
        double score = -dto.getRequestedPrice() * 1_000_000_000L
                - dto.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForZSet().add(getBuyKey(dto.getStockCode()), json, score); // key - value - score 순서
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매수 주문 JSON 직렬화 실패", e);
        }
    }

    /** Redis에서 가장 우선순위 높은 매수 주문 꺼내기 **/
    public Optional<StockOrderRedisDTO> popBestBuyOrder(String stockCode) {
        String key = getBuyKey(stockCode);
        Set<String> result = redisTemplate.opsForZSet().reverseRange(key, 0, 0);
        if (result == null || result.isEmpty()) return Optional.empty();

        String json = result.iterator().next();
        redisTemplate.opsForZSet().remove(key, json); // pop (key - value 형태로 맞는지 확인해야 함)
        try {
            return Optional.of(objectMapper.readValue(json, StockOrderRedisDTO.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매수 주문 JSON 역직렬화 실패", e);
        }
    }

    /** Redis에 매도 주문 저장 **/
    public void pushSellOrder(StockOrder order) {
        StockOrderRedisDTO dto = converter.toDTO(order);
        double score = dto.getRequestedPrice() * 1_000_000_000L
                + dto.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForZSet().add(getSellKey(dto.getStockCode()), json, score);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매도 주문 JSON 직렬화 실패", e);
        }
    }

    /** Redis에서 가장 우선순위 높은 매도 주문 꺼내기 **/
    public Optional<StockOrderRedisDTO> popBestSellOrder(String stockCode) {
        String key = getSellKey(stockCode);
        Set<String> result = redisTemplate.opsForZSet().range(key, 0, 0);
        if (result == null || result.isEmpty()) return Optional.empty();

        String json = result.iterator().next();
        redisTemplate.opsForZSet().remove(key, json); // pop
        try {
            return Optional.of(objectMapper.readValue(json, StockOrderRedisDTO.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매도 주문 JSON 역직렬화 실패", e);
        }
    }
}
