package com.fs.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FillDto(
        Long orderId,
        String figi,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal commission,
        LocalDateTime executedAt
) {
}
