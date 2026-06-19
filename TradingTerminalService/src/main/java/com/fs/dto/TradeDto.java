package com.fs.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeDto(
        Long id,
        Long userId,
        String figi,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal realizedPnl,
        Long orderId,
        BigDecimal commission,
        LocalDateTime executedAt
) {
}
