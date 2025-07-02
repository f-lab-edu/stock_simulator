package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Order;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl extends BaseRepositoryImpl<Order, Long> implements OrderRepository{
    public OrderRepositoryImpl(EntityManager em) {
        super(em, Order.class);
    }
}
