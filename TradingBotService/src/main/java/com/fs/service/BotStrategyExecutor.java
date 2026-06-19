package com.fs.service;

import com.fs.domain.BotStrategy;
import com.fs.domain.TradingBot;
import com.fs.strategy.*;
import com.fs.trading.core.TradeSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@Slf4j
public class BotStrategyExecutor {

    private final Map<BotStrategy, TradingStrategy> strategies;

    public BotStrategyExecutor(
            SmaCrossoverStrategyAdapter smaCrossoverStrategy,
            MacdCrossoverStrategyAdapter macdCrossoverStrategy,
            EmaTrendStrategyAdapter emaTrendStrategy,
            VolatilityBreakoutStrategyAdapter volatilityBreakoutStrategy) {
        strategies = new EnumMap<>(BotStrategy.class);
        strategies.put(BotStrategy.SMA_CROSSOVER, smaCrossoverStrategy);
        strategies.put(BotStrategy.MACD_CROSSOVER, macdCrossoverStrategy);
        strategies.put(BotStrategy.EMA_TREND, emaTrendStrategy);
        strategies.put(BotStrategy.VOLATILITY_BREAKOUT, volatilityBreakoutStrategy);
    }

    public TradeSignal evaluate(TradingBot bot, StrategyContext context) {
        log.debug("Оценка сигнала: botId={}, strategy={}, figi={}, candles.size={}, position={}",
                bot.getId(), bot.getStrategy(), bot.getFigi(),
                context.getCandles().size(), context.getPositionQuantity());
        TradingStrategy strategy = strategies.get(bot.getStrategy());
        if (strategy == null) {
            log.warn("Стратегия не найдена для бота {}: {}", bot.getId(), bot.getStrategy());
            return TradeSignal.HOLD;
        }
        try {
            TradeSignal signal = strategy.evaluate(bot, context);
            log.info("Торговый сигнал: botId={}, strategy={}, figi={}, signal={}, candles.size={}",
                    bot.getId(), bot.getStrategy(), bot.getFigi(), signal, context.getCandles().size());
            return signal;
        } catch (Exception e) {
            log.error("Ошибка при выполнении стратегии {} для бота {}: {}",
                    bot.getStrategy(), bot.getId(), e.getMessage());
            return TradeSignal.HOLD;
        }
    }
}
