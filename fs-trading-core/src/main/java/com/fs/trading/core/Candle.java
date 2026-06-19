package com.fs.trading.core;

import java.math.BigDecimal;

/**
 * Минимальная свеча для расчёта индикаторов (close — основное поле для SMA/EMA/MACD).
 */
public record Candle(BigDecimal close) {

    public Candle {
        if (close == null) {
            throw new IllegalArgumentException("close must not be null");
        }
    }
}
