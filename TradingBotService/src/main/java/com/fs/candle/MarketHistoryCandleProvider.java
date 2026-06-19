package com.fs.candle;

import com.fs.config.BotProperties;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.MarketHistoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketHistoryCandleProvider implements CandleHistoryProvider {

    private final MarketHistoryClient marketHistoryClient;
    private final BotProperties botProperties;

    @Override
    public List<PriceDataDto> getCandles(String figi) {
        int lookback = botProperties.getCandleLookback();
        String interval = botProperties.getCandleInterval();
        Instant to = Instant.now();
        Instant from = lookbackStart(to, lookback, interval);

        try {
            HistoricCandlesDto history = marketHistoryClient.getCandles(figi, from, to, interval, null);
            List<BrokerCandleDto> brokerCandles = history != null && history.getCandles() != null
                    ? history.getCandles()
                    : List.of();
            if (brokerCandles.isEmpty()) {
                log.debug("MarketHistory вернул пустой список свечей для FIGI: {}", figi);
                return List.of();
            }
            return mapAndTrim(brokerCandles, figi, lookback);
        } catch (Exception e) {
            log.warn("Ошибка получения свечей из MarketHistory для FIGI {}: {}", figi, e.getMessage());
            return List.of();
        }
    }

    static Instant lookbackStart(Instant to, int lookback, String interval) {
        String normalized = interval != null ? interval.toUpperCase() : "DAY";
        return switch (normalized) {
            case "HOUR", "1H" -> to.minus((long) lookback * 3, ChronoUnit.HOURS);
            case "MINUTE", "1M", "5M" -> to.minus((long) lookback * 5, ChronoUnit.MINUTES);
            default -> to.minus((long) lookback * 3, ChronoUnit.DAYS);
        };
    }

    private static List<PriceDataDto> mapAndTrim(List<BrokerCandleDto> brokerCandles, String figi, int lookback) {
        List<PriceDataDto> mapped = brokerCandles.stream()
                .filter(c -> c.getTime() != null && c.getClose() != null)
                .sorted(Comparator.comparing(BrokerCandleDto::getTime))
                .map(c -> new PriceDataDto(
                        figi,
                        c.getClose(),
                        LocalDateTime.ofInstant(c.getTime(), ZoneOffset.UTC)))
                .toList();

        if (mapped.size() <= lookback) {
            return mapped;
        }
        return mapped.subList(mapped.size() - lookback, mapped.size());
    }
}
