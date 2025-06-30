package com.portfolio2025.first.domain;


import com.portfolio2025.first.domain.order.OrderStatus;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "executed_quantity")
    private Long executedQuantity;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    // ✅ Order ↔ StockOrder 연결
    // cascade?? orphanRemoval???
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockOrder> stockOrders = new ArrayList<>();

    public static Order createBuyOrder(User user, Long totalPrice) {
        return null;
    }
}

