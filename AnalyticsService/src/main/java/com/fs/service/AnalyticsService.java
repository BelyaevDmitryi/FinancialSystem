package com.fs.service;

import com.fs.dto.*;
import com.fs.feignclient.PriceServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private final PriceServiceClient priceServiceClient;
    private final Random random = new Random();

    /**
     * Минимальное число точек для построения серии на графике (число цен = period * CHART_MULTIPLIER).
     */
    private static final int CHART_MULTIPLIER = 3;

    /**
     * Вычисляет Simple Moving Average (SMA) и серию значений для графика.
     */
    public SmaResponseDto calculateSMA(AnalyticsRequestDto request) {
        List<PriceDataDto> prices = request.getPriceData();
        int period = request.getPeriod();
        int pointsForChart = Math.max(period, period * CHART_MULTIPLIER);

        // Если priceData не предоставлен, получаем текущую цену и генерируем историю
        if (prices == null || prices.isEmpty()) {
            prices = generatePriceHistory(request.getFigi(), pointsForChart);
        }

        if (prices.size() < period) {
            throw new IllegalArgumentException("Недостаточно данных для расчета SMA. Требуется минимум " + period + " точек данных.");
        }

        List<PriceDataDto> sortedPrices = prices.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());

        // Скользящая SMA для каждой точки (начиная с индекса period - 1)
        List<BigDecimal> smaValues = new ArrayList<>();
        for (int i = period - 1; i < sortedPrices.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(sortedPrices.get(j).getPrice());
            }
            smaValues.add(sum.divide(BigDecimal.valueOf(period), MATH_CONTEXT));
        }

        BigDecimal sma = smaValues.isEmpty() ? BigDecimal.ZERO : smaValues.get(smaValues.size() - 1);

        return new SmaResponseDto(
                request.getFigi(),
                sma,
                LocalDateTime.now(),
                period,
                smaValues
        );
    }

    /**
     * Вычисляет Exponential Moving Average (EMA) и серию значений для графика.
     */
    public EmaResponseDto calculateEMA(AnalyticsRequestDto request) {
        List<PriceDataDto> prices = request.getPriceData();
        int period = request.getPeriod();
        int pointsForChart = Math.max(period, period * CHART_MULTIPLIER);

        // Если priceData не предоставлен, получаем текущую цену и генерируем историю
        if (prices == null || prices.isEmpty()) {
            prices = generatePriceHistory(request.getFigi(), pointsForChart);
        }

        if (prices.size() < period) {
            throw new IllegalArgumentException("Недостаточно данных для расчета EMA. Требуется минимум " + period + " точек данных.");
        }

        List<PriceDataDto> sortedPrices = prices.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());

        BigDecimal multiplier = BigDecimal.valueOf(2.0)
                .divide(BigDecimal.valueOf(period + 1), MATH_CONTEXT);

        List<BigDecimal> emaValues = new ArrayList<>();
        BigDecimal ema = sortedPrices.get(0).getPrice();
        emaValues.add(ema);

        for (int i = 1; i < sortedPrices.size(); i++) {
            BigDecimal price = sortedPrices.get(i).getPrice();
            ema = price.multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier, MATH_CONTEXT), MATH_CONTEXT), MATH_CONTEXT);
            emaValues.add(ema);
        }

        return new EmaResponseDto(
                request.getFigi(),
                ema,
                LocalDateTime.now(),
                period,
                emaValues
        );
    }

    /**
     * Вычисляет волатильность (стандартное отклонение)
     */
    public VolatilityResponseDto calculateVolatility(AnalyticsRequestDto request) {
        List<PriceDataDto> prices = request.getPriceData();
        int period = request.getPeriod();

        // Если priceData не предоставлен, получаем текущую цену и генерируем историю
        if (prices == null || prices.isEmpty()) {
            prices = generatePriceHistory(request.getFigi(), period);
        }

        if (prices.size() < period) {
            throw new IllegalArgumentException("Недостаточно данных для расчета волатильности. Требуется минимум " + period + " точек данных.");
        }

        List<PriceDataDto> recentPrices = prices.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(period)
                .collect(Collectors.toList());

        BigDecimal mean = recentPrices.stream()
                .map(PriceDataDto::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), MATH_CONTEXT);

        BigDecimal variance = recentPrices.stream()
                .map(p -> p.getPrice().subtract(mean, MATH_CONTEXT))
                .map(diff -> diff.multiply(diff, MATH_CONTEXT))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), MATH_CONTEXT);

        BigDecimal standardDeviation = sqrt(variance);
        BigDecimal volatility = standardDeviation.divide(mean, MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(100), MATH_CONTEXT);

        return new VolatilityResponseDto(
                request.getFigi(),
                volatility,
                period,
                standardDeviation
        );
    }

    /**
     * Вычисляет MACD (Moving Average Convergence Divergence)
     */
    public MacdResponseDto calculateMACD(AnalyticsRequestDto request) {
        List<PriceDataDto> prices = request.getPriceData();

        // Если priceData не предоставлен, получаем текущую цену и генерируем историю
        if (prices == null || prices.isEmpty()) {
            prices = generatePriceHistory(request.getFigi(), 26);
        }

        if (prices.size() < 26) {
            throw new IllegalArgumentException("Недостаточно данных для расчета MACD. Требуется минимум 26 точек данных.");
        }

        int fastPeriod = 12;
        int slowPeriod = 26;
        int signalPeriod = 9;

        AnalyticsRequestDto fastRequest = new AnalyticsRequestDto(request.getFigi(), prices, fastPeriod);
        AnalyticsRequestDto slowRequest = new AnalyticsRequestDto(request.getFigi(), prices, slowPeriod);

        BigDecimal fastEMA = calculateEMA(fastRequest).getEma();
        BigDecimal slowEMA = calculateEMA(slowRequest).getEma();

        BigDecimal macd = fastEMA.subtract(slowEMA, MATH_CONTEXT);

        List<PriceDataDto> macdPrices = new ArrayList<>();
        for (int i = 0; i < signalPeriod && i < prices.size(); i++) {
            macdPrices.add(new PriceDataDto(request.getFigi(), macd, prices.get(i).getTimestamp()));
        }

        AnalyticsRequestDto signalRequest = new AnalyticsRequestDto(request.getFigi(), macdPrices, signalPeriod);
        BigDecimal signal = calculateEMA(signalRequest).getEma();

        BigDecimal histogram = macd.subtract(signal, MATH_CONTEXT);

        return new MacdResponseDto(
                request.getFigi(),
                macd,
                signal,
                histogram,
                LocalDateTime.now()
        );
    }

    /**
     * Вычисляет квадратный корень (метод Ньютона)
     */
    private BigDecimal sqrt(BigDecimal value) {
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

    /**
     * Генерирует историю цен на основе текущей цены
     * Если не удается получить текущую цену, использует значение по умолчанию
     */
    private List<PriceDataDto> generatePriceHistory(String figi, int period) {
        log.info("Генерация истории цен для FIGI: {}, период: {}", figi, period);
        
        BigDecimal basePrice;
        try {
            // Пытаемся получить текущую цену из PriceService
            List<PriceDataDto> currentPrices = priceServiceClient.getPrices(Collections.singletonList(figi));
            if (currentPrices != null && !currentPrices.isEmpty()) {
                basePrice = currentPrices.get(0).getPrice();
                log.info("Получена текущая цена для FIGI {}: {}", figi, basePrice);
            } else {
                // Используем значение по умолчанию
                basePrice = BigDecimal.valueOf(100.0);
                log.warn("Не удалось получить цену для FIGI {}, используется значение по умолчанию: {}", figi, basePrice);
            }
        } catch (Exception e) {
            // В случае ошибки используем значение по умолчанию
            basePrice = BigDecimal.valueOf(100.0);
            log.warn("Ошибка при получении цены для FIGI {}: {}. Используется значение по умолчанию: {}", 
                    figi, e.getMessage(), basePrice);
        }

        // Генерируем историю цен с небольшими вариациями
        List<PriceDataDto> priceHistory = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = period - 1; i >= 0; i--) {
            // Генерируем цену с небольшим случайным отклонением (±5%)
            double variation = (random.nextDouble() - 0.5) * 0.1; // от -5% до +5%
            BigDecimal price = basePrice.multiply(BigDecimal.valueOf(1.0 + variation), MATH_CONTEXT);
            LocalDateTime timestamp = now.minusDays(i);
            
            priceHistory.add(new PriceDataDto(figi, price, timestamp));
        }
        
        log.info("Сгенерировано {} точек исторических данных для FIGI {}", priceHistory.size(), figi);
        return priceHistory;
    }
}
