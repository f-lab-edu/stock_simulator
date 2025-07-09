package com.portfolio2025.first.domain.stock;

/**
 * [StockOrderStatus → OrderStatus 상태 집계 규칙]
 *
 * 예:
 * - PENDING -> OrderStatus = CREATED (초기 생성)
 * - ALL FILLED → OrderStatus = COMPLETED
 * - SOME FILLED + SOME PENDING → OrderStatus = PROCESSING
 * - ALL CANCELLED → OrderStatus = CANCELLED
 * - MIXED (FILLED + CANCELLED) → OrderStatus = COMPLETED
 */



public enum StockOrderStatus {
    PENDING, // 진행 중
    PARTIALLY_FILLED, // 일부 체결
    FILLED, // 완료
    CANCELLED, // 취소
    EXPIRED, // 유효 시간 초과로 인한 만료
}
