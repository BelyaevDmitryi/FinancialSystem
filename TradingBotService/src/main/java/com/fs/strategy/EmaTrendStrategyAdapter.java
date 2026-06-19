package com.fs.strategy;

import com.fs.domain.TradingBot;
import com.fs.trading.core.EmaTrendStrategy;
import com.fs.trading.core.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class EmaTrendStrategyAdapter implements TradingStrategy {

    private final EmaTrendStrategy coreStrategy;

    public EmaTrendStrategyAdapter(EmaTrendStrategy coreStrategy) {
        this.coreStrategy = coreStrategy;
    }

    @Override
    public TradeSignal evaluate(TradingBot bot, StrategyContext context) {
        if (bot.getEmaPeriod() == null) {
            return TradeSignal.HOLD;
        }
        return coreStrategy.evaluate(
                StrategyCandleMapper.fromPriceData(context.getCandles()),
                context.getPositionQuantity(),
                bot.getEmaPeriod());
    }
}
