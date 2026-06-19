package com.fs.service;

import com.fs.config.DashboardProperties;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.MarketHistoryClient;
import com.fs.feignclient.PriceServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceBaselineService {

    private final DashboardProperties dashboardProperties;
    private final PriceServiceClient priceServiceClient;
    private final MarketHistoryClient marketHistoryClient;

    public Map<String, BigDecimal> resolveBaselinePrices(List<String> figies) {
        if (figies == null || figies.isEmpty()) {
            return Map.of();
        }

        try {
            return switch (dashboardProperties.getPriceBaseline()) {
                case MARKET_HISTORY_D1 -> resolveFromMarketHistory(figies);
                case REDIS_SNAPSHOT -> resolveFromRedisSnapshot(figies);
            };
        } catch (Exception e) {
            log.warn("Не удалось получить baseline цены ({}): {}. daily change = 0",
                    dashboardProperties.getPriceBaseline(), e.getMessage());
            return Map.of();
        }
    }

    private Map<String, BigDecimal> resolveFromRedisSnapshot(List<String> figies) {
        return priceServiceClient.getSnapshotPrices(figies).stream()
                .collect(Collectors.toMap(PriceDataDto::getFigi, PriceDataDto::getPrice, (left, right) -> left));
    }

    private Map<String, BigDecimal> resolveFromMarketHistory(List<String> figies) {
        Map<String, BigDecimal> result = new HashMap<>();
        Instant to = Instant.now();
        Instant from = to.minus(5, ChronoUnit.DAYS);

        for (String figi : figies) {
            try {
                HistoricCandlesDto history = marketHistoryClient.getCandles(figi, from, to, "DAY", null);
                BigDecimal previousClose = extractPreviousClose(history);
                if (previousClose != null) {
                    result.put(figi, previousClose);
                }
            } catch (Exception e) {
                log.warn("MarketHistory недоступен для FIGI {}: {}", figi, e.getMessage());
            }
        }

        if (result.isEmpty()) {
            log.warn("MarketHistory не вернул baseline цены для FIGI {}. daily change = 0", figies);
        }
        return result;
    }

    static BigDecimal extractPreviousClose(HistoricCandlesDto history) {
        if (history == null || history.getCandles() == null || history.getCandles().isEmpty()) {
            return null;
        }

        List<BrokerCandleDto> sorted = history.getCandles().stream()
                .filter(candle -> candle.getClose() != null && candle.getTime() != null)
                .sorted(Comparator.comparing(BrokerCandleDto::getTime))
                .toList();

        if (sorted.isEmpty()) {
            return null;
        }
        if (sorted.size() == 1) {
            return sorted.get(0).getClose();
        }
        return sorted.get(sorted.size() - 2).getClose();
    }
}
