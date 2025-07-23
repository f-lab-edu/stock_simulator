package com.portfolio2025.first.listener;

import com.portfolio2025.first.dto.StockOrderRedisDTO;
import com.portfolio2025.first.dto.event.TradeSavedEvent;
import com.portfolio2025.first.service.RedisStockOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 체결 객체 생성 이후 Redis 반영 위한 event 발행
 * 후속 처리 가능
 * Transaction commit 이후 동기적으로 이벤트 발행 처리하기 - AFTER_COMMIT
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeRedisSyncListener {

    private final RedisStockOrderService redisStockOrderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTradeSavedEvent(TradeSavedEvent event) {
        log.info("[RedisSync] TradeSavedEvent 수신 - tradeId: {}", event.getTradeId());

        StockOrderRedisDTO buyDTO = event.getBuyDTO();
        StockOrderRedisDTO sellDTO = event.getSellDTO();

        if (buyDTO != null && buyDTO.hasQuantity()) {
            redisStockOrderService.pushBuyOrderDTO(buyDTO);
            log.info("[RedisSync] BuyOrder 재삽입 - {}", buyDTO);
        }

        if (sellDTO != null && sellDTO.hasQuantity()) {
            redisStockOrderService.pushSellOrderDTO(sellDTO);
            log.info("[RedisSync] SellOrder 재삽입 - {}", sellDTO);
        }
    }
}
