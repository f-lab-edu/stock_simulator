package com.portfolio2025.first.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    private Long amount;

    public Money(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
        this.amount = amount;
    }

    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money minus(Money other) {
        if (this.amount < other.amount) {
            throw new IllegalArgumentException("잔액이 부족합니다");
        }
        return new Money(this.amount - other.amount);
    }

    public boolean isLowerThan(Money money) {
        return (this.amount < money.amount);
    }

    public boolean isHigherThan(Money money) {
        return (this.amount > money.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return String.valueOf(amount);
    }


}
