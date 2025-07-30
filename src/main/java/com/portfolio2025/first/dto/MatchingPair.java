package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRedisDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Matching 진행 시 후보로 선정된 대상들
 * [07.26]
 * (수정)
 *
 * [고민]
 *
 */
@RequiredArgsConstructor
@Getter
public class MatchingPair {
    private final StockOrderRedisDTO buyDTO; // REDIS에서 추출된 매수 DTO
    private final StockOrderRedisDTO sellDTO; // REDIS에서 추출된 매도 DTO

    // Refactoring 대상으로 진행해야 하지 않는지? - DTO가 가진 책임이 이렇게 진행되도 괜찮은지?
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
