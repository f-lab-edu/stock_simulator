package com.portfolio2025.first.domain;


import com.portfolio2025.first.domain.order.OrderStatus;
import com.portfolio2025.first.domain.order.OrderType;
import com.portfolio2025.first.domain.stock.StockOrder;
import com.portfolio2025.first.domain.stock.StockOrderStatus;
import com.portfolio2025.first.domain.vo.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

/**
 * 매수 Or 매도 주문 StockOrder의 상위 정보를 관리하는 Order
 * [07.26]
 * (수정)
 *
 * [고민]
 * 1. CREATED, PROCESSING 상태를 명확하게 구분할 수 있어야 함
 */
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
    private OrderStatus orderStatus; // CREATED(생성 완료) - PROCESSING(진행중 + 부분체결) - COMPLETED(완료) - CANCELLED(삭제완료)

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;  // BUY / SELL 등

    @Embedded
    @AttributeOverride(name = "moneyValue", column = @Column(name = "total_price"))
    private Money totalPrice;  // 하위 StockOrder 총 주문 금액

    @Column(name = "deleted", nullable = false)
    private Boolean deleted; // 삭제 여부

    @Column(name = "created_at", nullable = false) // nullable 설정했다면 - primitive 사용 가능함
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 양방향 연관관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockOrder> stockOrders = new ArrayList<>();


    @Builder
    private Order(User user, OrderType orderType, Money totalPrice) {
        this.user = user;
        this.orderStatus = OrderStatus.CREATED;
        this.orderType = orderType;
        this.totalPrice = totalPrice;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Order createSingleBuyOrder(Portfolio portfolio, StockOrder stockOrder, OrderType orderType,
                                             Money totalPrice) {
        Order createdOrder = Order.builder()
                .user(portfolio.getUser())
                .orderType(orderType) // 매도, 매수
                .totalPrice(totalPrice)
                .build();

        // 양방향 설정하기
        addStockOrder(stockOrder, createdOrder);
        return createdOrder;
    }

    /** 양방향 연관관계 편의 메서드 **/
    private static void addStockOrder(StockOrder stockOrder, Order createdOrder) {
        createdOrder.getStockOrders().add(stockOrder);
        stockOrder.setOrder(createdOrder);
    }

    /** 상태 변경 시 시간 갱신 */
    public void updateStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateExecutedTime(LocalDateTime executedTime) {
        this.updatedAt = executedTime;
    }

    // 고민 1. CREATED, PROCESSING 상태를 명확하게 구분할 수 있어야 함
    public void aggregateStatusFromStockOrders() {
        if (stockOrders.isEmpty()) return;

        boolean allFilled = stockOrders.stream().allMatch(so -> so.getStockOrderStatus() == StockOrderStatus.FILLED);
        boolean allCancelled = stockOrders.stream().allMatch(so -> so.getStockOrderStatus() == StockOrderStatus.CANCELLED);
        boolean allPending = stockOrders.stream().allMatch(so -> so.getStockOrderStatus() == StockOrderStatus.PENDING);

        if (allFilled || (hasFilled() && hasCancelled())) {
            updateStatus(OrderStatus.COMPLETED);
        } else if (allCancelled) {
            updateStatus(OrderStatus.CANCELED);
        } else {
            updateStatus(OrderStatus.PROCESSING);
        }
    }

    // 부분 체결인지 확인하는 로직
    private boolean hasFilled() {
        return stockOrders.stream()
                .anyMatch(so -> so.getStockOrderStatus() == StockOrderStatus.FILLED);
    }

    private boolean hasCancelled() {
        return stockOrders.stream()
                .anyMatch(so -> so.getStockOrderStatus() == StockOrderStatus.CANCELLED);
    }

}

