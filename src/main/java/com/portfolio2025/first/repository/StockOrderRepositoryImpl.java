package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.Order;
import com.portfolio2025.first.domain.stock.StockOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockOrderRepositoryImpl extends BaseRepositoryImpl<StockOrder, Long> implements StockOrderRepository {
    public StockOrderRepositoryImpl(EntityManager em) {
        super(em, StockOrder.class);
    }

    public void save(StockOrder stockOrder, Order order) {
        em.persist(stockOrder);
    }

    /** StockOrder - order (-user) - portfolio **/
    @Override
    public Optional<StockOrder> findByIdWithAllRelations(Long stockOrderId) {
        return em.createQuery("""
                SELECT so FROM StockOrder so
                JOIN FETCH so.order o
                JOIN FETCH o.user u
                JOIN FETCH so.portfolio p
                JOIN FETCH p.user pu
                WHERE so.id = :id
            """, StockOrder.class)
                .setParameter("id", stockOrderId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }
}
