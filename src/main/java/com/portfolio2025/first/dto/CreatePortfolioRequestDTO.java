package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.PortfolioType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 포트폴리오 생성시 입력 데이터
 */

@Getter
@Setter
@AllArgsConstructor
public class CreatePortfolioRequestDTO {
    private PortfolioType portfolioType;

}
