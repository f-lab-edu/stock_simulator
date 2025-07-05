package com.portfolio2025.first.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.stock.StockOrder;
import java.util.List;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

/** 주문 저장을 담당하는 서비스 **/
public class TradeQueueService {
    private final Jedis jedis = new Jedis("localhost", 6379); // Redis 서버 주소와 포트
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 매수 주문 저장 **/
    public void pushBuyStockOrder(StockOrder stockOrder) {
        String redisKey = "BUY_QUEUE_" + stockOrder.getStock().getStockCode();
        double score = -stockOrder.getRequestedPrice().getMoneyValue();  // 높은 가격이 먼저 나오게 음수화
        try {
            String value = objectMapper.writeValueAsString(stockOrder);  // JSON 직렬화
            jedis.zadd(redisKey, score, value);  // Redis SortedSet에 저장
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Order JSON 직렬화 실패", e);
        }
    }

    /** 매도 주문 저장 **/
    public void pushSellOrder(StockOrder stockOrder) {
        String redisKey = "SELL_QUEUE_" + stockOrder.getId();
        double score = stockOrder.getRequestedPrice().getMoneyValue();  // 낮은 가격이 먼저 오게

        try {
            String value = objectMapper.writeValueAsString(stockOrder);
            jedis.zadd(redisKey, score, value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Order JSON 직렬화 실패", e);
        }
    }

    /** 가장 낮은 금액 가진 매수 주문 꺼내기 **/
    public Order popBestSellOrder(String stockCode) {
        String redisKey = "SELL_QUEUE_" + stockCode;

        List<Tuple> result = jedis.zpopmin(redisKey, 1);// 가장 낮은 가격 1개 꺼냄
        if (result.isEmpty()) return null;

        String orderJson = result.iterator().next().getElement(); // JSON 문자열
        try {
            return objectMapper.readValue(orderJson, Order.class); // JSON → 객체
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis Order 역직렬화 실패", e);
        }
    }
}
