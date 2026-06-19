package com.fs.service;

import com.fs.domain.StoredCandle;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.feign.BrokerIntegrationClient;
import com.fs.repository.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MarketHistoryService {

    private static final Logger log = LoggerFactory.getLogger(MarketHistoryService.class);

    private final BrokerIntegrationClient brokerIntegrationClient;
    private final CandleRepository candleRepository;

    public MarketHistoryService(
            BrokerIntegrationClient brokerIntegrationClient,
            CandleRepository candleRepository) {
        this.brokerIntegrationClient = brokerIntegrationClient;
        this.candleRepository = candleRepository;
    }

  /**
   * DB-first: при полном покрытии диапазона [from, to] возвращает свечи из БД.
   * Иначе — fallback к BrokerIntegrationService (без автосохранения в БД).
   */
    public HistoricCandlesDto fetchCandles(String figi, Instant from, Instant to, String interval, String broker) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Параметр from должен быть не позже to");
        }
        List<StoredCandle> stored = candleRepository.findByFigiAndIntervalAndTimeBetweenOrderByTimeAsc(
                figi, interval, from, to);
        if (hasSufficientCoverage(stored, from, to)) {
            log.debug("Свечи figi={} {}..{} из БД ({} баров)", figi, from, to, stored.size());
            return toHistoricDto(figi, interval, stored);
        }
        log.debug("Свечи figi={} {}..{} — fallback к брокеру", figi, from, to);
        return brokerIntegrationClient.getHistoricCandles(figi, from, to, interval, broker);
    }

    private static boolean hasSufficientCoverage(List<StoredCandle> stored, Instant from, Instant to) {
        if (stored.isEmpty()) {
            return false;
        }
        Instant first = stored.get(0).getTime();
        Instant last = stored.get(stored.size() - 1).getTime();
        return !first.isAfter(from) && !last.isBefore(to);
    }

    private static HistoricCandlesDto toHistoricDto(String figi, String interval, List<StoredCandle> stored) {
        List<BrokerCandleDto> candles = stored.stream()
                .map(MarketHistoryService::toBrokerCandle)
                .toList();
        return new HistoricCandlesDto(figi, interval, candles);
    }

    private static BrokerCandleDto toBrokerCandle(StoredCandle stored) {
        return new BrokerCandleDto(
                stored.getTime(),
                stored.getOpen(),
                stored.getHigh(),
                stored.getLow(),
                stored.getClose(),
                stored.getVolume());
    }
}
