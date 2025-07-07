package com.portfolio2025.first.converter;

import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.dto.StockOrderRedisDTO;

public class StockOrderRedisConverter {

    public StockOrderRedisDTO toDTO(StockOrder entity) {
        return new StockOrderRedisDTO(
                entity.getId(),
                entity.getStock().getStockCode(),
                entity.getRequestedPrice().getMoneyValue(),
                entity.getRemainedQuantity().getQuantityValue(),
                entity.getCreatedAt()
        );
    }

    // 필요시 DTO → Entity 로 복원하는 로직도 추가 가능
}
