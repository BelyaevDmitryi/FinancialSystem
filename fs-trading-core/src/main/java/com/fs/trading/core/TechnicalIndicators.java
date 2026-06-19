package com.fs.trading.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * In-process расчёт индикаторов (паритет с AnalyticsService для сигналов стратегий).
 */
public final class TechnicalIndicators {

    static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int MACD_MIN_CANDLES = 26;
    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;

    private TechnicalIndicators() {
    }

    public static BigDecimal calculateSma(List<Candle> candles, int period) {
        if (candles.size() < period) {
            throw new IllegalArgumentException("Недостаточно свечей для SMA: " + candles.size());
        }
        int from = candles.size() - period;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = from; i < candles.size(); i++) {
            sum = sum.add(candles.get(i).close());
        }
        return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateEma(List<Candle> candles, int period) {
        if (candles.size() < period) {
            throw new IllegalArgumentException("Недостаточно свечей для EMA: " + candles.size());
        }
        BigDecimal multiplier = BigDecimal.valueOf(2.0)
                .divide(BigDecimal.valueOf(period + 1), MATH_CONTEXT);
        BigDecimal ema = candles.get(0).close();
        for (int i = 1; i < candles.size(); i++) {
            BigDecimal price = candles.get(i).close();
            ema = price.multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT);
        }
        return ema;
    }

    public static MacdResult calculateMacd(List<Candle> candles) {
        if (candles.size() < MACD_MIN_CANDLES) {
            throw new IllegalArgumentException("Недостаточно свечей для MACD: " + candles.size());
        }
        BigDecimal fastEma = calculateEma(candles, MACD_FAST);
        BigDecimal slowEma = calculateEma(candles, MACD_SLOW);
        BigDecimal macd = fastEma.subtract(slowEma, MATH_CONTEXT);

        List<Candle> macdSeries = new ArrayList<>();
        for (int i = 0; i < MACD_SIGNAL && i < candles.size(); i++) {
            macdSeries.add(new Candle(macd));
        }
        BigDecimal signal = calculateEma(macdSeries, MACD_SIGNAL);
        BigDecimal histogram = macd.subtract(signal, MATH_CONTEXT);
        return new MacdResult(macd, signal, histogram);
    }

    public static BigDecimal calculateVolatilityPercent(List<Candle> candles, int period) {
        if (candles.size() < period) {
            throw new IllegalArgumentException("Недостаточно свечей для волатильности: " + candles.size());
        }
        int from = candles.size() - period;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = from; i < candles.size(); i++) {
            sum = sum.add(candles.get(i).close());
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(period), MATH_CONTEXT);

        BigDecimal variance = BigDecimal.ZERO;
        for (int i = from; i < candles.size(); i++) {
            BigDecimal diff = candles.get(i).close().subtract(mean, MATH_CONTEXT);
            variance = variance.add(diff.multiply(diff, MATH_CONTEXT), MATH_CONTEXT);
        }
        variance = variance.divide(BigDecimal.valueOf(period), MATH_CONTEXT);

        BigDecimal stdDev = sqrt(variance);
        return stdDev.divide(mean, MATH_CONTEXT).multiply(BigDecimal.valueOf(100), MATH_CONTEXT);
    }

    private static BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal x = value;
        BigDecimal prevX;
        do {
            prevX = x;
            x = value.divide(x, MATH_CONTEXT)
                    .add(x, MATH_CONTEXT)
                    .divide(BigDecimal.valueOf(2), MATH_CONTEXT);
        } while (x.subtract(prevX, MATH_CONTEXT).abs().compareTo(BigDecimal.valueOf(0.0001)) > 0);
        return x;
    }

    public record MacdResult(BigDecimal macd, BigDecimal signal, BigDecimal histogram) {
    }
}
