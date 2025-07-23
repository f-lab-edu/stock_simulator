package com.portfolio2025.first.dto.event;

import com.portfolio2025.first.dto.StockOrderRedisDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TradeSavedEvent {
    private final Long tradeId;
    private final StockOrderRedisDTO buyDTO;
    private final StockOrderRedisDTO sellDTO;
}
