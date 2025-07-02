package com.portfolio2025.first.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    private Long moneyValue;

    public Money(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
        this.moneyValue = amount;
    }

    public Money plus(Money other) {
        return new Money(this.moneyValue + other.moneyValue);
    }

    public Money minus(Money other) {
        if (isLowerThan(other)) {
            throw new IllegalArgumentException("잔액이 부족합니다");
        }
        return new Money(this.moneyValue - other.moneyValue);
    }

    public boolean isLowerThan(Money money) {
        return (this.moneyValue < money.moneyValue);
    }

    public boolean isHigherThan(Money money) {
        return (this.moneyValue > money.moneyValue);
    }


    @Override
    public String toString() {
        return String.valueOf(moneyValue);
    }
}
