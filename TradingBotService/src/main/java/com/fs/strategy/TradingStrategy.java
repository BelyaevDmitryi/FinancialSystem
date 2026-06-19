package com.fs.strategy;

import com.fs.domain.TradingBot;
import com.fs.trading.core.TradeSignal;

public interface TradingStrategy {

    TradeSignal evaluate(TradingBot bot, StrategyContext context);
}
