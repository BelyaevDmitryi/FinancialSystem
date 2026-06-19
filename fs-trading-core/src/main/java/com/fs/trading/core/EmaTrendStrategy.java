package com.fs.trading.core;

import java.math.BigDecimal;
import java.util.List;

public class EmaTrendStrategy implements TradingStrategy {

    @Override
    public TradeSignal evaluate(List<Candle> candlesUpToBar, BigDecimal positionQty, int emaPeriod) {
        if (emaPeriod <= 0 || candlesUpToBar.size() < emaPeriod) {
            return TradeSignal.HOLD;
        }
        BigDecimal ema = TechnicalIndicators.calculateEma(candlesUpToBar, emaPeriod);
        BigDecimal close = candlesUpToBar.get(candlesUpToBar.size() - 1).close();
        BigDecimal qty = positionQty != null ? positionQty : BigDecimal.ZERO;

        if (close.compareTo(ema) > 0 && qty.signum() == 0) {
            return TradeSignal.BUY;
        }
        if (close.compareTo(ema) < 0 && qty.signum() > 0) {
            return TradeSignal.SELL;
        }
        return TradeSignal.HOLD;
    }
}
