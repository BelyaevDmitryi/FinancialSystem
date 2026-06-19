package com.fs.service;

import com.fs.domain.StoredCandle;
import com.fs.dto.HistoricCandlesDto;
import com.fs.feign.BrokerIntegrationClient;
import com.fs.repository.CandleRepository;
import com.fs.support.MarketHistoryServiceIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Transactional
class MarketHistoryServiceDbFirstIntegrationTest extends MarketHistoryServiceIntegrationTestBase {

    private static final String FIGI = "BBG004730N88";
    private static final String INTERVAL = "DAY";
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-03T00:00:00Z");

    @Autowired
    private MarketHistoryService marketHistoryService;

    @Autowired
    private CandleRepository candleRepository;

    @MockBean
    private BrokerIntegrationClient brokerIntegrationClient;

    @BeforeEach
    void cleanCandles() {
        candleRepository.deleteAll();
    }

    @Test
    void fetchCandles_returnsFromDbWhenRangeFullyCovered() {
        seedBar(Instant.parse("2026-01-01T00:00:00Z"), 100);
        seedBar(Instant.parse("2026-01-02T00:00:00Z"), 101);
        seedBar(Instant.parse("2026-01-03T00:00:00Z"), 102);
        candleRepository.flush();

        HistoricCandlesDto dto = marketHistoryService.fetchCandles(FIGI, FROM, TO, INTERVAL, null);

        assertThat(dto.getCandles()).hasSize(3);
        assertThat(dto.getCandles().get(0).getClose()).isEqualByComparingTo("100");
        verify(brokerIntegrationClient, never()).getHistoricCandles(any(), any(), any(), any(), any());
    }

    @Test
    void fetchCandles_fallsBackToBrokerWhenDbEmpty() {
        HistoricCandlesDto brokerDto = new HistoricCandlesDto(FIGI, INTERVAL, List.of());
        org.mockito.Mockito.when(brokerIntegrationClient.getHistoricCandles(
                eq(FIGI), eq(FROM), eq(TO), eq(INTERVAL), eq(null))).thenReturn(brokerDto);

        HistoricCandlesDto dto = marketHistoryService.fetchCandles(FIGI, FROM, TO, INTERVAL, null);

        assertThat(dto).isSameAs(brokerDto);
        verify(brokerIntegrationClient).getHistoricCandles(FIGI, FROM, TO, INTERVAL, null);
    }

    @Test
    void fetchCandles_fallsBackToBrokerWhenDbPartial() {
        seedBar(Instant.parse("2026-01-02T00:00:00Z"), 101);
        candleRepository.flush();
        HistoricCandlesDto brokerDto = new HistoricCandlesDto(FIGI, INTERVAL, List.of());
        org.mockito.Mockito.when(brokerIntegrationClient.getHistoricCandles(
                eq(FIGI), eq(FROM), eq(TO), eq(INTERVAL), eq(null))).thenReturn(brokerDto);

        HistoricCandlesDto dto = marketHistoryService.fetchCandles(FIGI, FROM, TO, INTERVAL, null);

        assertThat(dto).isSameAs(brokerDto);
        verify(brokerIntegrationClient).getHistoricCandles(FIGI, FROM, TO, INTERVAL, null);
    }

    private void seedBar(Instant time, double close) {
        candleRepository.upsert(new StoredCandle(
                null,
                FIGI,
                INTERVAL,
                time,
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(close + 1),
                BigDecimal.valueOf(close - 1),
                BigDecimal.valueOf(close),
                1000L));
    }
}
