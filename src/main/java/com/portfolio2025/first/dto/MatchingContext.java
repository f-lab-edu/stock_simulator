package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 구체 체결 정보를 관리하는 MatchingContext
 * [07.26]
 * (수정)
 *
 * [고민]
 *
 */
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
