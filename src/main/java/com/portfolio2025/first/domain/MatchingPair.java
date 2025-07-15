package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MatchingPair {
    private final StockOrderRedisDTO buyDTO;
    private final StockOrderRedisDTO sellDTO;

    public boolean isPriceMatchable() {
        return buyDTO.getRequestedPrice() >= sellDTO.getRequestedPrice();
    }

    public boolean isNotPriceMatchable() {
        return !(isPriceMatchable());
    }

    public Quantity getExecutableQuantity() {
        return new Quantity(Math.min(buyDTO.getRemainQuantity(), sellDTO.getRemainQuantity()));
    }

    public MatchingPair afterExecution(Quantity executableQuantity) {
        StockOrderRedisDTO updatedBuyDTO = buyDTO.afterExecution(executableQuantity);
        StockOrderRedisDTO updatedSellDTO = sellDTO.afterExecution(executableQuantity);

        return new MatchingPair(updatedBuyDTO, updatedSellDTO);
    }
}
