package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MatchingContext {
    private final StockOrder buyOrder;
    private final StockOrder sellOrder;
    private final Portfolio buyPortfolio;
    private final Portfolio sellPortfolio;
    private final Stock stock;
    private final Quantity executableQuantity;
    private final Money executablePrice;
}
