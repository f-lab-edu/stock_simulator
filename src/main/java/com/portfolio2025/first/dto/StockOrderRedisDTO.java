package com.portfolio2025.first.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderRedisDTO {
    // 기존의 StockOrder 수정되는 경우 -> 기존 주문 삭제하고 새로운 StockOrderRedisDTO 반영하는 걸로
    private Long id; // stockOrderId
    private String stockCode; // 종목
    private Long requestedPrice; // 원 단위
    private Long remainQuantity; // 미체결 수량

    private LocalDateTime createdAt; // 주문 건 시간


}

