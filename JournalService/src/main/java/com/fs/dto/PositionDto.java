package com.fs.dto;

import java.math.BigDecimal;

public record PositionDto(
        Long userId,
        String figi,
        BigDecimal quantity,
        BigDecimal avgPrice
) {
}
