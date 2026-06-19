package com.fs.strategy;

import com.fs.domain.TradingBot;
import com.fs.trading.core.VolatilityBreakoutStrategy;
import com.fs.trading.core.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class VolatilityBreakoutStrategyAdapter implements TradingStrategy {

    private final VolatilityBreakoutStrategy coreStrategy;

    public VolatilityBreakoutStrategyAdapter(VolatilityBreakoutStrategy coreStrategy) {
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
