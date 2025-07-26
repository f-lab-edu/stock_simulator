package com.portfolio2025.first.dto;

import lombok.Getter;

/**
 * 매수 or 매도 주문 생성 시 입력 데이터
 */

@Getter
public class StockOrderRequestDTO {
    private String stockCode; // 종목 조회용 stockCode
    private Long requestedQuantity; // 주문 시 희망 수량
    private Long requestedPrice; // 주문 시 희망 가격
    private Long userId; // 유저 정보 (추후 로그인 세션 활용해서 대체 예정)

    public StockOrderRequestDTO(String stockCode, Long requestedQuantity, Long requestedPrice, Long userId) {
        this.stockCode = stockCode;
        this.requestedQuantity = requestedQuantity;
        this.requestedPrice = requestedPrice;
        this.userId = userId;
    }
}
