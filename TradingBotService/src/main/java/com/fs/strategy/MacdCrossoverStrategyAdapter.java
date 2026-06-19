package com.fs.strategy;

import com.fs.domain.TradingBot;
import com.fs.trading.core.MacdCrossoverStrategy;
import com.fs.trading.core.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class MacdCrossoverStrategyAdapter implements TradingStrategy {

    private final MacdCrossoverStrategy coreStrategy;

    public MacdCrossoverStrategyAdapter(MacdCrossoverStrategy coreStrategy) {
        this.coreStrategy = coreStrategy;
    }

    @Override
    public TradeSignal evaluate(TradingBot bot, StrategyContext context) {
        return coreStrategy.evaluate(
                StrategyCandleMapper.fromPriceData(context.getCandles()),
                context.getPositionQuantity(),
                0);
    }
}
