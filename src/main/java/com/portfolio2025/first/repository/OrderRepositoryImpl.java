package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Order;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl extends BaseRepositoryImpl<Order, Long> implements OrderRepository{
    public OrderRepositoryImpl(EntityManager em) {
        super(em, Order.class);
    }

    /** 부모 엔티티 조회시 JOIN FETCH - N+1 방지하기 위함 **/
    @Override
    public Optional<Order> findByIdWithStockOrders(Long orderId) {
        List<Order> result = em.createQuery(
                        "SELECT DISTINCT o FROM Order o " +
                                "JOIN FETCH o.stockOrders so " +
                                "JOIN FETCH so.stock " +
                                "WHERE o.id = :orderId", Order.class)
                .setParameter("orderId", orderId)
                .getResultList();

        return result.stream().findFirst();
    }
}
