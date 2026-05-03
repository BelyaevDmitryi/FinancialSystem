package com.fs.service;

import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.OptimizationResultDto;
import com.fs.dto.SmaGridOptimizationRequest;
import com.fs.feign.MarketHistoryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SmaTrendOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(SmaTrendOptimizationService.class);

    private final MarketHistoryClient marketHistoryClient;

    public SmaTrendOptimizationService(MarketHistoryClient marketHistoryClient) {
        this.marketHistoryClient = marketHistoryClient;
    }

    public OptimizationResultDto optimize(SmaGridOptimizationRequest request) {
        if (request.getFrom().isAfter(request.getTo())) {
            throw new IllegalArgumentException("from должен быть не позже to");
        }
        if (request.getPeriodMax() < request.getPeriodMin()) {
            throw new IllegalArgumentException("periodMax должен быть >= periodMin");
        }

        String interval = request.getInterval() != null ? request.getInterval() : "DAY";
        HistoricCandlesDto history = marketHistoryClient.getCandles(
                request.getFigi(),
                request.getFrom(),
                request.getTo(),
                interval,
                null
        );

        List<BrokerCandleDto> candles = history.getCandles();
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Нет свечей за выбранный период");
        }

        List<BigDecimal> closes = candles.stream().map(BrokerCandleDto::getClose).toList();
        if (closes.size() < request.getPeriodMax() + 2) {
            throw new IllegalArgumentException("Недостаточно точек цены для периода " + request.getPeriodMax());
        }

        int bestPeriod = request.getPeriodMin();
        double bestScore = Double.NEGATIVE_INFINITY;
        int trials = 0;

        for (int p = request.getPeriodMin(); p <= request.getPeriodMax(); p += request.getPeriodStep()) {
            double score = trendScoreAboveSma(closes, p);
            trials++;
            if (score > bestScore) {
                bestScore = score;
                bestPeriod = p;
            }
        }

        String description = "Максимизирована доля баров, где close > SMA(period) по ценам закрытия.";
        log.info("Оптимизация завершена: bestPeriod={}, score={}", bestPeriod, bestScore);
        return new OptimizationResultDto(
                request.getFigi(),
                bestPeriod,
                bestScore,
                trials,
                description
        );
    }

    private double trendScoreAboveSma(List<BigDecimal> closes, int period) {
        int hits = 0;
        int counted = 0;
        for (int i = period; i < closes.size(); i++) {
            BigDecimal sma = simpleMovingAverage(closes, i - period, i);
            counted++;
            if (closes.get(i).compareTo(sma) > 0) {
                hits++;
            }
        }
        if (counted == 0) {
            return 0.0;
        }
        return (double) hits / counted;
    }

    private BigDecimal simpleMovingAverage(List<BigDecimal> closes, int startInclusive, int endExclusive) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int j = startInclusive; j < endExclusive; j++) {
            sum = sum.add(closes.get(j));
        }
        int n = endExclusive - startInclusive;
        return sum.divide(BigDecimal.valueOf(n), 8, RoundingMode.HALF_UP);
    }
}
