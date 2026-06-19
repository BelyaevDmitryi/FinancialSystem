package com.fs.service;

import com.fs.config.DashboardProperties;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.MarketHistoryClient;
import com.fs.feignclient.PriceServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceBaselineServiceTest {

    @Mock
    private PriceServiceClient priceServiceClient;

    @Mock
    private MarketHistoryClient marketHistoryClient;

    private DashboardProperties dashboardProperties;
    private PriceBaselineService priceBaselineService;

    @BeforeEach
    void setUp() {
        dashboardProperties = new DashboardProperties();
        priceBaselineService = new PriceBaselineService(
                dashboardProperties,
                priceServiceClient,
                marketHistoryClient
        );
    }

    @Test
    void resolveBaselinePrices_usesRedisSnapshotByDefault() {
        dashboardProperties.setPriceBaseline(DashboardProperties.PriceBaseline.REDIS_SNAPSHOT);
        when(priceServiceClient.getSnapshotPrices(List.of("FIGI1")))
                .thenReturn(List.of(new PriceDataDto("FIGI1", BigDecimal.valueOf(95), null)));

        Map<String, BigDecimal> result = priceBaselineService.resolveBaselinePrices(List.of("FIGI1"));

        assertThat(result).containsEntry("FIGI1", BigDecimal.valueOf(95));
    }

    @Test
    void resolveBaselinePrices_whenMarketHistoryUnavailable_returnsEmptyMap() {
        dashboardProperties.setPriceBaseline(DashboardProperties.PriceBaseline.MARKET_HISTORY_D1);
        when(marketHistoryClient.getCandles(
                org.mockito.ArgumentMatchers.eq("FIGI1"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("DAY"),
                org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new RuntimeException("service unavailable"));

        Map<String, BigDecimal> result = priceBaselineService.resolveBaselinePrices(List.of("FIGI1"));

        assertThat(result).isEmpty();
    }

    @Test
    void extractPreviousClose_usesSecondToLastCandle() {
        HistoricCandlesDto history = new HistoricCandlesDto();
        history.setCandles(List.of(
                candle(Instant.parse("2026-06-16T00:00:00Z"), BigDecimal.valueOf(90)),
                candle(Instant.parse("2026-06-17T00:00:00Z"), BigDecimal.valueOf(95)),
                candle(Instant.parse("2026-06-18T00:00:00Z"), BigDecimal.valueOf(100))
        ));

        assertThat(PriceBaselineService.extractPreviousClose(history))
                .isEqualByComparingTo(BigDecimal.valueOf(95));
    }

    private static BrokerCandleDto candle(Instant time, BigDecimal close) {
        BrokerCandleDto candle = new BrokerCandleDto();
        candle.setTime(time);
        candle.setClose(close);
        return candle;
    }
}
