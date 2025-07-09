package com.portfolio2025.first.dto;

import com.portfolio2025.first.domain.PortfolioType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePortfolioRequestDTO {
    private PortfolioType portfolioType;

}
