package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@AllArgsConstructor
@Builder
public class CurrencyRate {
    private String charCode;
    private BigDecimal value;
}
