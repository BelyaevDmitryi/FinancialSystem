package com.fs.strategy;

import com.fs.domain.TradingBot;
import com.fs.trading.core.SmaCrossoverStrategy;
import com.fs.trading.core.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class SmaCrossoverStrategyAdapter implements TradingStrategy {

    private final SmaCrossoverStrategy coreStrategy;

    public SmaCrossoverStrategyAdapter(SmaCrossoverStrategy coreStrategy) {
        this.coreStrategy = coreStrategy;
    }

    @Override
    public TradeSignal evaluate(TradingBot bot, StrategyContext context) {
        if (bot.getSmaPeriod() == null) {
            return TradeSignal.HOLD;
        }
        return coreStrategy.evaluate(
                StrategyCandleMapper.fromPriceData(context.getCandles()),
                context.getPositionQuantity(),
                bot.getSmaPeriod());
    }
}
