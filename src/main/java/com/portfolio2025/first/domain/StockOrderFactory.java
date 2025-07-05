package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.stock.StockOrderStatus;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import com.portfolio2025.first.dto.StockOrderRequestDTO;
import com.portfolio2025.first.repository.StockRepository;
import org.springframework.stereotype.Component;

@Component
public class StockOrderFactory {
    private static StockRepository stockRepository;

    public StockOrder createStockOrder(StockOrderRequestDTO request) {
        Stock stock = stockRepository.findByStockCode(request.getStockCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다."));

        Quantity quantity = new Quantity(request.getRequestedQuantity());
        Money orderPrice = new Money(quantity.getQuantityValue() * request.getRequestedPrice());

        stock.validateSufficientQuantity(quantity);

        return StockOrder.createStockOrder(stock, quantity, orderPrice, StockOrderStatus.PENDING);
    }



}
