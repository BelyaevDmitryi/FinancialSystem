package com.fs.trading.core;

import java.math.BigDecimal;
import java.util.List;

/**
 * SMA crossover: close &gt; SMA → BUY (без позиции); close &lt; SMA → SELL (с позицией).
 */
public class SmaCrossoverStrategy implements TradingStrategy {

    @Override
    public TradeSignal evaluate(List<Candle> candlesUpToBar, BigDecimal positionQty, int smaPeriod) {
        if (smaPeriod <= 0 || candlesUpToBar.size() < smaPeriod) {
            return TradeSignal.HOLD;
        }
        BigDecimal sma = TechnicalIndicators.calculateSma(candlesUpToBar, smaPeriod);
        BigDecimal close = candlesUpToBar.get(candlesUpToBar.size() - 1).close();
        BigDecimal qty = positionQty != null ? positionQty : BigDecimal.ZERO;

        if (close.compareTo(sma) > 0 && qty.signum() == 0) {
            return TradeSignal.BUY;
        }
        if (close.compareTo(sma) < 0 && qty.signum() > 0) {
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }
}
