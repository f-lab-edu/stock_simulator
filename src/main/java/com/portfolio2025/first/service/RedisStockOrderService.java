package com.portfolio2025.first.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.domain.MatchingPair;
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
 * remove 가 반영되는 경우에 분산 락 고려해야 함 -- 아직 구현 못한 상황 (유의)
 */


@Service
@RequiredArgsConstructor
public class RedisStockOrderService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private String getBuyKey(String stockCode) {
        return "BUY_QUEUE_" + stockCode;
    }

    private String getSellKey(String stockCode) {
        return "SELL_QUEUE_" + stockCode;
    }

    /** Redis에 매수 주문 저장 (SortedSet - ZADD) **/
    public void pushBuyOrder(StockOrder order) {
        StockOrderRedisDTO dto = StockOrderRedisDTO.from(order);
        double score = -dto.getRequestedPrice() * 1_000_000_000L
                - dto.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForZSet().add(getBuyKey(dto.getStockCode()), json, score); // key - value - score 순서
            System.out.println("pushBuyOrderfinished");
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
        StockOrderRedisDTO dto = StockOrderRedisDTO.from(order);
        double score = dto.getRequestedPrice() * 1_000_000_000L
                + dto.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForZSet().add(getSellKey(dto.getStockCode()), json, score);
            System.out.println("pushSellOrderfinished");
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

    /** 수정된 매도 주문 다시 올리기 (기존 데이터 삭제 - 새로 추가) **/
    public void pushModifiedSellOrder(StockOrderRedisDTO sellStockOrderDTO) {
        String key = getSellKey(sellStockOrderDTO.getStockCode());
        // Redisson 락 고려하지 않은 상황
        try {
            String json = objectMapper.writeValueAsString(sellStockOrderDTO);
            redisTemplate.opsForZSet().remove(key, json);
            // 점수, 새롭게 추가
            double score = sellStockOrderDTO.getRequestedPrice() * 1_000_000_000L
                    + sellStockOrderDTO.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            redisTemplate.opsForZSet().add(key, json, score);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매도 주문 JSON 직렬화 실패", e);
        }
    }

     /** key 안에 value 해당되면 삭제하기) **/
    public void removeBuyOrder(StockOrderRedisDTO buyStockOrderDTO) {
        // Redisson 락 고려하지 않은 상황
        String key = getBuyKey(buyStockOrderDTO.getStockCode());
        try {
            String json = objectMapper.writeValueAsString(buyStockOrderDTO);
            redisTemplate.opsForZSet().remove(key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매수 주문 JSON 직렬화 실패", e);
        }

    }

    public Optional<MatchingPair> popMatchPair(String stockCode) {
        Optional<StockOrderRedisDTO> buyStockOrderDTO = popBestBuyOrder(stockCode);
        Optional<StockOrderRedisDTO> sellStockOrderDTO = popBestSellOrder(stockCode);

        boolean isBuyEmpty = buyStockOrderDTO.isEmpty();
        boolean isSellEmpty = sellStockOrderDTO.isEmpty();

        if (isBuyEmpty && isSellEmpty) {
            System.out.println("[popMatchPair] 매수/매도 모두 없음");
            return Optional.empty();
        }

        if (isBuyEmpty) {
            // 매도 주문만 있음 → 복원
            pushSellOrderDTO(sellStockOrderDTO.get());
            System.out.println("[popMatchPair] 매수 없음 → 매도 주문 복원");
            return Optional.empty();
        }

        if (isSellEmpty) {
            // 매수 주문만 있음 → 복원
            pushBuyOrderDTO(buyStockOrderDTO.get());
            System.out.println("[popMatchPair] 매도 없음 → 매수 주문 복원");
            return Optional.empty();
        }

        // 둘 다 존재 → MatchingPair 반환
        return Optional.of(new MatchingPair(buyStockOrderDTO.get(), sellStockOrderDTO.get()));
    }

    public void pushBuyOrderDTO(StockOrderRedisDTO buyDTO) {
        double score = -buyDTO.getRequestedPrice() * 1_000_000_000L
                - buyDTO.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        try {
            String json = objectMapper.writeValueAsString(buyDTO);
            redisTemplate.opsForZSet().add(getBuyKey(buyDTO.getStockCode()), json, score); // key - value - score 순서
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매수 주문 JSON 직렬화 실패", e);
        }
    }

    public void pushSellOrderDTO(StockOrderRedisDTO sellDTO) {
        double score = sellDTO.getRequestedPrice() * 1_000_000_000L
                + sellDTO.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        try {
            String json = objectMapper.writeValueAsString(sellDTO);
            redisTemplate.opsForZSet().add(getSellKey(sellDTO.getStockCode()), json, score); // key - value - score 순서
        } catch (JsonProcessingException e) {
            throw new RuntimeException("매수 주문 JSON 직렬화 실패", e);
        }
    }

    public void pushBack(MatchingPair pair) {
        pushBuyOrderDTO(pair.getBuyDTO());
        pushSellOrderDTO(pair.getSellDTO());
    }
}
