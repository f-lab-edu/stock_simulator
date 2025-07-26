package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Money;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자

    @Column(name = "bank_name", nullable = false)
    private String bankName; // 은행명

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber; // 계좌번호

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // TimeStamp - 개설 시각

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // TimeStamp - 수정 시각

    @Column(name = "is_active", nullable = false)
    private Boolean isActive; // 활성화 여부

    @Embedded
    @Column(name = "available_cash", nullable = false)
    private Money availableCash; // Portfolio에서 인출 가능한 금액 (계좌 -> User availableCash -> Portfoio availableCash)

    @Column(name = "user_name")
    private String userName; // 사용자 이름

    @Builder
    private Account(User user, String bankName, String accountNumber, String userName) {
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
        this.availableCash = new Money(10_000_000L); // 천 만원
    }

    public static Account createAccount(User user, String bankName, String accountNumber, String userName) {
        return Account.builder()
                .user(user)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .userName(userName)
                .build();
    }

    public void withdraw(Money money) {
        this.availableCash = availableCash.minus(money);
    }

    public void deposit(Money money) {
        this.availableCash = availableCash.plus(money);
    }

    public boolean isLowerThan(Money money) {
        return availableCash.isLowerThan(money);
    }
}
