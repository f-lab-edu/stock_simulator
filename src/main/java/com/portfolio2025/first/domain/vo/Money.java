package com.portfolio2025.first.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래 단위(돈) Money
 * [07.26]
 * (수정) requireNonNull 메서드 추가(null-checking 담당하는 메서드 추출)
 * (추가) Comparable - compareTo 메서드 구현
 *
 * [고민]
 *
 */
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
        requireNonNull(other);
        return new Money(this.moneyValue + other.moneyValue);
    }

    public Money minus(Money other) {
        requireNonNull(other);

        if (isLowerThan(other)) {
            throw new IllegalArgumentException("잔액이 부족합니다");
        }
        return new Money(this.moneyValue - other.moneyValue);
    }

    private void requireNonNull(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("인자를 확인해주세요.");
        }
    }

    public boolean isLowerThan(Money money) {
        return (this.moneyValue < money.moneyValue);
    }

    public boolean isHigherThan(Money money) {
        return (this.moneyValue > money.moneyValue);
    }

    public Money multiply(Quantity quantity) {
        return new Money(moneyValue * quantity.getQuantityValue());
    }

    public int compareTo(Money other) {
        return this.moneyValue.compareTo(other.moneyValue);
    }

    @Override
    public String toString() {
        return String.valueOf(moneyValue);
    }
}
