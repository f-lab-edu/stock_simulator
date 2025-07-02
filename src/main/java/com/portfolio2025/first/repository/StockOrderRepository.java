package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.stock.StockOrder;

public interface StockOrderRepository extends BaseRepository<StockOrder, Long>{
    // Override (양방향 연관관계, 서로 추가해줘야 함)
    public void save(StockOrder stockOrder, Order order);
}
