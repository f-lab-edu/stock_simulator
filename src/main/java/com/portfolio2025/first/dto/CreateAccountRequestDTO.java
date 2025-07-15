package com.portfolio2025.first.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 계좌 생성 요청 시 입력 데이터
 */

@Getter
@Setter
@AllArgsConstructor
public class CreateAccountRequestDTO {
    private String bankName;
    private String accountNumber;
    private String userName;
}
