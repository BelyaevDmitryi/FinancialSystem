package com.fs.trading.core;

import java.math.BigDecimal;
import java.util.List;

/**
 * Offline-first контракт стратегии: live и backtest используют одну реализацию.
 *
 * @param period smaPeriod или emaPeriod; для MACD/Volatility может игнорироваться.
 */
public interface TradingStrategy {

    TradeSignal evaluate(List<Candle> candlesUpToBar, BigDecimal positionQty, int period);
}
