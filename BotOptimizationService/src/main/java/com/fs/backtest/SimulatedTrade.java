package com.fs.backtest;

import java.math.BigDecimal;
import java.time.Instant;

public record SimulatedTrade(
        Instant time,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal realizedPnl
) {
}
