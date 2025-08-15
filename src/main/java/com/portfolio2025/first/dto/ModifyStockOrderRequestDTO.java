package com.portfolio2025.first.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModifyStockOrderRequestDTO {
    private Long stockOrderId;
    private Long userId;
    private Long requestedQuantity;
    private Long requestedPrice;
}
