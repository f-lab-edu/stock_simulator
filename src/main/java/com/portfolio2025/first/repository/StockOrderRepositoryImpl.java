package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.stock.StockOrder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class StockOrderRepositoryImpl extends BaseRepositoryImpl<StockOrder, Long> implements StockOrderRepository {
    public StockOrderRepositoryImpl(EntityManager em) {
        super(em, StockOrder.class);
    }

    public void save(StockOrder stockOrder, Order order) {
        // 양방향 고려한 편의 메서드 설정 (다 클래스 연관관계 추가하는 방향으로 설정)
        order.getStockOrders().add(stockOrder);
        stockOrder.setOrder(order);
        em.persist(stockOrder);
    }


}
