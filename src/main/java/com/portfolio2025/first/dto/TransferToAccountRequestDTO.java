package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.vo.Money;
import lombok.Getter;

@Getter
public class TransferToAccountRequestDTO {
    private String accountNumber;
    private Long portfolioId;
    private Long amount;

    public Money toMoney() {
        return new Money(amount);
    }

}

