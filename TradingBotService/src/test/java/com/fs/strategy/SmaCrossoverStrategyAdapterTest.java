package com.fs.strategy;

import com.fs.domain.BotStrategy;
import com.fs.domain.TradingBot;
import com.fs.dto.PriceDataDto;
import com.fs.trading.core.SmaCrossoverStrategy;
import com.fs.trading.core.TradeSignal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmaCrossoverStrategyAdapterTest {

    private final SmaCrossoverStrategyAdapter strategy =
            new SmaCrossoverStrategyAdapter(new SmaCrossoverStrategy());

    @Test
    void evaluate_priceAboveSma_noPosition_returnsBuy() {
        TradingBot bot = smaBot();
        List<PriceDataDto> candles = buildCandles(20, BigDecimal.valueOf(100));
        StrategyContext context = new DefaultStrategyContext(candles, BigDecimal.ZERO);

        assertThat(strategy.evaluate(bot, context)).isEqualTo(TradeSignal.BUY);
    }

    @Test
    void evaluate_priceBelowSmaWithPosition_returnsSell() {
        TradingBot bot = smaBot();
        List<PriceDataDto> candles = buildDecliningCandles(20, BigDecimal.valueOf(120));
        StrategyContext context = new DefaultStrategyContext(candles, BigDecimal.valueOf(3));

        assertThat(strategy.evaluate(bot, context)).isEqualTo(TradeSignal.SELL);
    }

    @Test
    void evaluate_priceBelowSmaWithoutPosition_returnsHold() {
        TradingBot bot = smaBot();
        List<PriceDataDto> candles = buildDecliningCandles(20, BigDecimal.valueOf(120));
        StrategyContext context = new DefaultStrategyContext(candles, BigDecimal.ZERO);

        assertThat(strategy.evaluate(bot, context)).isEqualTo(TradeSignal.HOLD);
    }

    private static TradingBot smaBot() {
        TradingBot bot = new TradingBot();
        bot.setId(1L);
        bot.setFigi("BBG004730N88");
        bot.setStrategy(BotStrategy.SMA_CROSSOVER);
        bot.setSmaPeriod(20);
        return bot;
    }

    private static List<PriceDataDto> buildCandles(int count, BigDecimal startPrice) {
        List<PriceDataDto> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            candles.add(new PriceDataDto(
                    "BBG004730N88",
                    startPrice.add(BigDecimal.valueOf(i)),
                    LocalDateTime.now().minusMinutes(count - i)));
        }
        return candles;
    }

    private static List<PriceDataDto> buildDecliningCandles(int count, BigDecimal startPrice) {
        List<PriceDataDto> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            candles.add(new PriceDataDto(
                    "BBG004730N88",
                    startPrice.subtract(BigDecimal.valueOf(i)),
                    LocalDateTime.now().minusMinutes(count - i)));
        }
        return candles;
    }
}
