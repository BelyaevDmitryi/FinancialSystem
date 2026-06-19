package com.fs.trading.core;

import java.math.BigDecimal;
import java.util.List;

public class VolatilityBreakoutStrategy implements TradingStrategy {

    private static final int MIN_CANDLES = 20;
    private static final BigDecimal VOLATILITY_THRESHOLD = BigDecimal.valueOf(5);

    @Override
    public TradeSignal evaluate(List<Candle> candlesUpToBar, BigDecimal positionQty, int ignoredPeriod) {
        if (candlesUpToBar.size() < MIN_CANDLES) {
            return TradeSignal.HOLD;
        }
        BigDecimal volatility = TechnicalIndicators.calculateVolatilityPercent(candlesUpToBar, MIN_CANDLES);
        BigDecimal qty = positionQty != null ? positionQty : BigDecimal.ZERO;

        if (volatility.compareTo(VOLATILITY_THRESHOLD) > 0) {
            return TradeSignal.BUY;
        }
        if (volatility.compareTo(VOLATILITY_THRESHOLD) <= 0 && qty.signum() > 0) {
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }
}
