package com.portfolio2025.first.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        if (otherQuantity == null) {
            throw new IllegalArgumentException("인자를 확인해주세요.");
        }
        return new Quantity(this.quantityValue + otherQuantity.getQuantityValue());
    }

    public Quantity minus(Quantity otherQuantity) {
        if (otherQuantity == null) {
            throw new IllegalArgumentException("인자를 확인해주세요.");
        }

        if (isLowerThan(otherQuantity)) {
            throw new IllegalArgumentException("잔여 수량이 부족합니다.");
        }

        return new Quantity(this.quantityValue - otherQuantity.getQuantityValue());
    }

    public boolean isLowerThan(Quantity otherQuantity) {
        return ((quantityValue - otherQuantity.getQuantityValue()) < 0);
    }

    @Override
    public String toString() {
        return "Quantity{" +
                "quantityValue=" + quantityValue +
                '}';
    }
}
