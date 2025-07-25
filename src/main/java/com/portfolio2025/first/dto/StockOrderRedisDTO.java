package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Redis에 반영된, 혹은 Redis로 부터 가지고 온 데이터 구조 클래스
 * 기존 StockOrder 수정되는 경우 -> 기존 주문 삭제하고 새로운 StockOrderRedisDTO 반영하는 걸로
 */

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StockOrderRedisDTO {

    private Long id; // stockOrderId
    private String stockCode; // 종목
    private Long requestedPrice; // 원 단위
    private Long remainQuantity; // 미체결 수량

    private LocalDateTime createdAt; // 주문 건 시간

    public static StockOrderRedisDTO from(StockOrder order) {
        return new StockOrderRedisDTO(
                order.getId(),
                order.getStock().getStockCode(),
                order.getRequestedPrice().getMoneyValue(),
                order.getRemainedQuantity().getQuantityValue(),
                order.getCreatedAt()
        );
    }

    public StockOrderRedisDTO afterExecution(Quantity executableQuantity) {
        Long updatedQuantityValue = remainQuantity - executableQuantity.getQuantityValue();

        return new StockOrderRedisDTO(
                this.id,
                this.stockCode,
                this.requestedPrice,
                updatedQuantityValue,
                this.createdAt
        );
    }

    public boolean hasQuantity() {
        return (remainQuantity > 0);
    }
}

