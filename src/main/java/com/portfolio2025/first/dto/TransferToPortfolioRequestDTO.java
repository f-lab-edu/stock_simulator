package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.vo.Money;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferToPortfolioRequestDTO {
    // 조회 시 필요한 값들 생각하기
    private String accountNumber; // account 조회 시 사용할 account number
    private Long portfolioId; // portfolio 조회 시 사용할 portfolioId
    private Long amount; // 단위: 원

    public Money toMoney() {
        return new Money(amount);
    }
}
