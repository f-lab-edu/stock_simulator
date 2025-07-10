package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.PortfolioType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreatePortfolioRequestDTO {
    private PortfolioType portfolioType;

}
