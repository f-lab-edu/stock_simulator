package com.portfolio2025.first.domain;


import com.portfolio2025.first.domain.order.OrderStatus;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.Stock;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.vo.Money;
import com.portfolio2025.first.domain.vo.Quantity;
import jakarta.persistence.AttributeOverride;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;  // BUY / SELL 등

    @Column(name = "total_price", nullable = false)
    @AttributeOverride(name = "moneyValue", column = @Column(name = "total_price"))
    private Money totalPrice;  // 하위 StockOrder 총 주문 금액

    @Column(name = "deleted", nullable = false)
    private Boolean deleted; // 삭제 여부

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 양방향 연관관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockOrder> stockOrders = new ArrayList<>();


    @Builder
    private Order(User user, OrderType orderType, Money totalPrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.user = user;
        this.orderStatus = OrderStatus.CREATED;
        this.orderType = orderType;
        this.orderType = orderType;
        this.totalPrice = totalPrice;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Order createBuyOrder(User user, StockOrder stockOrder, OrderType orderType,
                                       Money totalPrice) {
        Order createdOrder = Order.builder()
                .user(user)
                .orderType(orderType)
                .totalPrice(totalPrice)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return createdOrder;
    }

    /** 편의 메서드: StockOrder 추가 시 자동 관계 설정 */
//    public void addStockOrder(StockOrder stockOrder) {
//        stockOrders.add(stockOrder);
//        stockOrder.setOrder(this);
//    }

    /** StockOrder 변경 시 totalPrice 재계산 (예: 주문 추가/삭제 시) */
//    public void updateTotalPrice() {
//        this.totalPrice = stockOrders.stream()
//                .mapToLong(so -> so.getRequestedQuantity() * so.getRequestedPrice())
//                .sum();
//        this.updatedAt = LocalDateTime.now();
//    }

    /** 상태 변경 시 시간 갱신 */
    public void updateStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}

