package com.portfolio2025.first.service;

import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPrepareService {

    private final RedisStockOrderService redisStockOrderService;

    // 주문 생성이 모두 완료되었다면 최종 검증 진행하고 Redis 데이터 반영하기
    public void validateAndRegisterToRedis(StockOrder stockOrder, OrderType orderType) {
        if (stockOrder == null || stockOrder.getStock() == null) {
            log.warn("[OrderPrepareService] 유효하지 않은 주문입니다: stockOrderId={}",
                    stockOrder != null ? stockOrder.getId() : null);
            throw new IllegalArgumentException("Invalid StockOrder");
        }

        if (orderType == OrderType.BUY) {
            redisStockOrderService.pushBuyOrder(stockOrder);
        } else if (orderType == OrderType.SELL) {
            redisStockOrderService.pushSellOrder(stockOrder);
        } else {
            throw new IllegalArgumentException("지원하지 않는 주문 타입: " + orderType);
        }

        log.info("[OrderPrepareService] Redis 오더북 등록 완료 - stockOrderId={}, type={}",
                stockOrder.getId(), orderType);
    }
}
