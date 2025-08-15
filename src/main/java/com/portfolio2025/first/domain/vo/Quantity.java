package com.portfolio2025.first.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래 단위(수량) Quantity
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
public class Quantity {

    private Long quantityValue;

    public Quantity(Long quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("잘못된 수량입니다.");
        }
        this.quantityValue = quantity;
    }

    public Quantity plus(Quantity otherQuantity) {
        requireNonNull(otherQuantity);
        return new Quantity(this.quantityValue + otherQuantity.getQuantityValue());
    }

    public Quantity minus(Quantity otherQuantity) {
        requireNonNull(otherQuantity);

        if (isLowerThan(otherQuantity)) {
            throw new IllegalArgumentException("잔여 수량이 부족합니다.");
        }

        return new Quantity(this.quantityValue - otherQuantity.getQuantityValue());
    }

    public boolean isLowerThan(Quantity otherQuantity) {
        return ((quantityValue - otherQuantity.getQuantityValue()) < 0);
    }

    public boolean isZero() {
        return (this.quantityValue == 0);
    }

    public int compareTo(Quantity other) {
        return this.quantityValue.compareTo(other.quantityValue);
    }

    private void requireNonNull(Quantity otherQuantity) {
        if (otherQuantity == null) {
            throw new IllegalArgumentException("인자를 확인해주세요.");
        }
    }

    @Override
    public String toString() {
        return "Quantity{" +
                "quantityValue=" + quantityValue +
                '}';
    }
}
