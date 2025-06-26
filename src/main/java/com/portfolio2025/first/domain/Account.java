package com.portfolio2025.first.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 User 참조 (ManyToOne) - 지연 로딩으로 (실제 객체를 사용할 때 조회하겠다는 의미)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Embedded
    @Column(name = "available_cash", nullable = false)
    private Money availableCash;

    @Column(name = "user_name")
    private String userName;

    @Builder
    public Account(User user, String bankName, String accountNumber, String userName) {
        if (user == null || bankName == null || accountNumber == null) {
            throw new IllegalArgumentException("사용자, 은행명, 계좌번호는 필수입니다.");
        }
        // input
        this.user = user;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.userName = userName;

        // Initialization
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.availableCash = new Money(1000_0000_0000L);
    }

    public void withdraw(Money money) {
        this.availableCash = availableCash.minus(money);
    }

    public void deposit(Money money) {
        this.availableCash = availableCash.plus(money);
    }
}
