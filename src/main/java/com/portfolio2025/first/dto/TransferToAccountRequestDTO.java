package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.vo.Money;
import lombok.Getter;

/**
 * Portfolio -> Account로 돈 보낼 때 필요한 입력 데이터
 */

@Getter
public class TransferToAccountRequestDTO {
    private String accountNumber;
    private Long portfolioId;
    private Long amount;

    public Money toMoney() {
        return new Money(amount);
    }

}

