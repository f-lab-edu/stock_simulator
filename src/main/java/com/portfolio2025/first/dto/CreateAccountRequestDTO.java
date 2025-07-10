package com.portfolio2025.first.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateAccountRequestDTO {
    private String bankName;
    private String accountNumber;
    private String userName;
}
