package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;
    @Column(name = "user_id", nullable = false, length = 30)
    private String userId;

    private String location;
    @Column(name = "phone_number")
    private String phoneNumber;
    private String email;

    @Embedded
    private Money balance;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String name, String location, String phoneNumber, String email, String userId) {
        if (name == null || userId == null) {
            throw new IllegalArgumentException("이름과 아이디는 필수입니다.");
        }

        this.name = name;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.userId = userId;
        this.balance = new Money(0L);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContact(String phoneNumber, String email) {
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    public void chargeBalance(Money money) {
        this.balance = this.balance.plus(money);
        this.updatedAt = LocalDateTime.now();
    }

    public void deductBalance(Money money) {
        this.balance = this.balance.minus(money);
        this.updatedAt = LocalDateTime.now();
    }

    public void deposit(Money money) {
        this.balance = balance.plus(money);
    }

    public void withdraw(Money money) {
        this.balance = balance.minus(money);
    }

    public boolean isBalanceInsufficient(Money money) {
        return (balance.isLowerThan(money));
    }

    public void validateSufficientBalance(Money money) {
        if (isBalanceInsufficient(money)) {
            throw new IllegalArgumentException("Insufficient account balance");
        }
    }

    public void buy(Money totalPriceVO) {
        validateSufficientBalance(totalPriceVO);
        withdraw(totalPriceVO);
    }
}

