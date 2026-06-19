package com.fs.candle;

import com.fs.config.BotProperties;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.MarketHistoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketHistoryCandleProviderTest {

    private static final String FIGI = "BBG004730N88";

    @Mock
    private MarketHistoryClient marketHistoryClient;

    private BotProperties botProperties;
    private MarketHistoryCandleProvider provider;

    @BeforeEach
    void setUp() {
        botProperties = new BotProperties();
        botProperties.setCandleLookback(30);
        botProperties.setCandleInterval("DAY");
        provider = new MarketHistoryCandleProvider(marketHistoryClient, botProperties);
    }

    @Test
    void getCandles_returnsTrimmedClosePricesSortedByTime() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-02T00:00:00Z");
        List<BrokerCandleDto> bars = List.of(
                bar(t2, 102),
                bar(t1, 100));
        when(marketHistoryClient.getCandles(eq(FIGI), any(), any(), eq("DAY"), isNull()))
                .thenReturn(new HistoricCandlesDto(FIGI, "DAY", bars));

        List<PriceDataDto> result = provider.getCandles(FIGI);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.get(1).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(102));
        assertThat(result.get(0).getTimestamp())
                .isEqualTo(LocalDateTime.ofInstant(t1, ZoneOffset.UTC));
    }

    @Test
    void getCandles_whenMoreThanLookback_returnsLastLookbackBars() {
        botProperties.setCandleLookback(2);
        List<BrokerCandleDto> bars = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bars.add(bar(Instant.parse("2026-01-0" + (i + 1) + "T00:00:00Z"), 100 + i));
        }
        when(marketHistoryClient.getCandles(eq(FIGI), any(), any(), eq("DAY"), isNull()))
                .thenReturn(new HistoricCandlesDto(FIGI, "DAY", bars));

        List<PriceDataDto> result = provider.getCandles(FIGI);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(103));
        assertThat(result.get(1).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(104));
    }

    @Test
    void getCandles_whenFeignFails_returnsEmpty() {
        when(marketHistoryClient.getCandles(eq(FIGI), any(), any(), eq("DAY"), isNull()))
                .thenThrow(new RuntimeException("connection refused"));

        List<PriceDataDto> result = provider.getCandles(FIGI);

        assertThat(result).isEmpty();
    }

    @Test
    void getCandles_usesLookbackWindowForDayInterval() {
        when(marketHistoryClient.getCandles(eq(FIGI), any(), any(), eq("DAY"), isNull()))
                .thenReturn(new HistoricCandlesDto(FIGI, "DAY", List.of()));

        provider.getCandles(FIGI);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(marketHistoryClient).getCandles(eq(FIGI), fromCaptor.capture(), toCaptor.capture(), eq("DAY"), isNull());
        assertThat(toCaptor.getValue()).isAfter(fromCaptor.getValue());
        assertThat(fromCaptor.getValue()).isBefore(Instant.now());
    }

    private static BrokerCandleDto bar(Instant time, double close) {
        return new BrokerCandleDto(
                time,
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(close),
                1L);
    }
}
