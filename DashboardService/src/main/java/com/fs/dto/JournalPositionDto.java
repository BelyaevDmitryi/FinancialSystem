package com.fs.dto;

import java.math.BigDecimal;

public record JournalPositionDto(
        Long userId,
        String figi,
        BigDecimal quantity,
        BigDecimal avgPrice
) {
}
