package com.fs.trading.core;

import java.math.BigDecimal;
import java.util.List;

public class MacdCrossoverStrategy implements TradingStrategy {

    private static final int MIN_CANDLES = 26;

    @Override
    public TradeSignal evaluate(List<Candle> candlesUpToBar, BigDecimal positionQty, int ignoredPeriod) {
        if (candlesUpToBar.size() < MIN_CANDLES) {
            return TradeSignal.HOLD;
        }
        TechnicalIndicators.MacdResult macd = TechnicalIndicators.calculateMacd(candlesUpToBar);
        BigDecimal qty = positionQty != null ? positionQty : BigDecimal.ZERO;

        if (macd.histogram().compareTo(BigDecimal.ZERO) > 0
                && macd.macd().compareTo(macd.signal()) > 0) {
            return TradeSignal.BUY;
        }
        if (macd.histogram().compareTo(BigDecimal.ZERO) < 0
                && macd.macd().compareTo(macd.signal()) < 0
                && qty.signum() > 0) {
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }
}
