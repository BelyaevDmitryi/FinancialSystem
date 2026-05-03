package com.fs.service;

import com.fs.dto.HistoricCandlesDto;
import com.fs.feign.BrokerIntegrationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MarketHistoryService {

    private static final Logger log = LoggerFactory.getLogger(MarketHistoryService.class);

    private final BrokerIntegrationClient brokerIntegrationClient;

    public MarketHistoryService(BrokerIntegrationClient brokerIntegrationClient) {
        this.brokerIntegrationClient = brokerIntegrationClient;
    }

    public HistoricCandlesDto fetchCandles(String figi, Instant from, Instant to, String interval, String broker) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Параметр from должен быть не позже to");
        }
        log.debug("Запрос свечей figi={} {}..{} interval={}", figi, from, to, interval);
        return brokerIntegrationClient.getHistoricCandles(figi, from, to, interval, broker);
    }
}
