package com.portfolio2025.first.dto.event;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * StockOrder 생성시 Event 발행, 데이터 구조 형태
 * Consumer에서 인자로 받은 데이터 구조
 * MatchRequest, OrderPrepare
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private Long portfolioId;
    private String stockCode;
    private Long quantity;
    private Long price;
    private String orderType; // "BUY"
}
