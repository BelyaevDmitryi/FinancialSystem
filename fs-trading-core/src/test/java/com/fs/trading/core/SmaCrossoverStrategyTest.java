package com.fs.trading.core;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Детерминированные сигналы SMA на golden CSV (US-OSE-008 / US-OSE-007).
 */
class SmaCrossoverStrategyTest {

    private final SmaCrossoverStrategy strategy = new SmaCrossoverStrategy();

    @Test
    void goldenCsv_bar5_closeAboveSma_noPosition_returnsBuy() throws Exception {
        List<Candle> window = loadGoldenCandles().subList(0, 5);
        assertThat(strategy.evaluate(window, BigDecimal.ZERO, 5)).isEqualTo(TradeSignal.BUY);
    }

    @Test
    void goldenCsv_bar10_closeBelowSma_withPosition_returnsSell() throws Exception {
        List<Candle> window = loadGoldenCandles().subList(0, 10);
        assertThat(strategy.evaluate(window, BigDecimal.ONE, 5)).isEqualTo(TradeSignal.SELL);
    }

    @Test
    void goldenCsv_fullSeries_producesBuyThenSellRoundTrip() throws Exception {
        List<Candle> candles = loadGoldenCandles();
        int smaPeriod = 5;
        BigDecimal position = BigDecimal.ZERO;
        int buyBar = -1;
        int sellBar = -1;

        for (int i = 0; i < candles.size(); i++) {
            List<Candle> window = candles.subList(0, i + 1);
            TradeSignal signal = strategy.evaluate(window, position, smaPeriod);
            if (signal == TradeSignal.BUY && position.signum() == 0) {
                buyBar = i;
                position = BigDecimal.ONE;
            } else if (signal == TradeSignal.SELL && position.signum() > 0) {
                sellBar = i;
                position = BigDecimal.ZERO;
            }
        }

        assertThat(buyBar).isEqualTo(4);
        assertThat(sellBar).isEqualTo(9);
        assertThat(buyBar).isLessThan(sellBar);
    }

    @Test
    void evaluate_insufficientCandles_returnsHold() {
        List<Candle> candles = List.of(new Candle(BigDecimal.valueOf(100)));
        assertThat(strategy.evaluate(candles, BigDecimal.ZERO, 5)).isEqualTo(TradeSignal.HOLD);
    }

    private static List<Candle> loadGoldenCandles() throws Exception {
        List<Candle> candles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(SmaCrossoverStrategyTest.class.getResourceAsStream(
                        "/backtest/golden-sma.csv")),
                StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                candles.add(new Candle(new BigDecimal(parts[4])));
            }
        }
        return candles;
    }
}
