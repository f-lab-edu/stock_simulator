package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.stock.StockOrder;
import java.util.Optional;

public interface StockOrderRepository extends BaseRepository<StockOrder, Long>{
    // Override (양방향 연관관계, 서로 추가해줘야 함)
    void save(StockOrder stockOrder, Order order);

    /** PESSIMISTIC_WRITE 락 동반 **/
    Optional<StockOrder> findByIdWithAllRelations(Long id);
}
