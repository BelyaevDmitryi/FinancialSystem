package com.fs.service;

import com.fs.dto.*;
import com.fs.feignclient.PriceServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService")
class AnalyticsServiceTest {

    private static final String FIGI = "BBG004730N88";
    private static final int PERIOD = 5;

    @Mock
    private PriceServiceClient priceServiceClient;

    @InjectMocks
    private AnalyticsService analyticsService;

    private List<PriceDataDto> priceData;

    @BeforeEach
    void setUp() {
        LocalDateTime base = LocalDateTime.of(2025, 1, 1, 12, 0);
        priceData = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new PriceDataDto(FIGI, BigDecimal.valueOf(100 + i), base.plusDays(i)))
                .toList();
    }

    @Nested
    @DisplayName("calculateSMA")
    class CalculateSMA {

        @Test
        @DisplayName("возвращает корректный SMA при достаточном количестве данных")
        void shouldReturnSmaWhenEnoughData() {
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, priceData, PERIOD);

            SmaResponseDto result = analyticsService.calculateSMA(request);

            assertThat(result).isNotNull();
            assertThat(result.getFigi()).isEqualTo(FIGI);
            assertThat(result.getPeriod()).isEqualTo(PERIOD);
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getValues()).isNotNull().hasSize(6); // 10 цен, период 5 -> 6 значений SMA
            // Последние 5 цен: 106,107,108,109,110 -> среднее = 108
            assertThat(result.getSma()).isEqualByComparingTo(BigDecimal.valueOf(108));
        }

        @Test
        @DisplayName("выбрасывает исключение при недостатке данных")
        void shouldThrowWhenNotEnoughData() {
            List<PriceDataDto> fewPrices = priceData.subList(0, 2);
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, fewPrices, PERIOD);

            assertThatThrownBy(() -> analyticsService.calculateSMA(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Недостаточно данных для расчета SMA");
        }
    }

    @Nested
    @DisplayName("calculateEMA")
    class CalculateEMA {

        @Test
        @DisplayName("возвращает корректный EMA при достаточном количестве данных")
        void shouldReturnEmaWhenEnoughData() {
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, priceData, PERIOD);

            EmaResponseDto result = analyticsService.calculateEMA(request);

            assertThat(result).isNotNull();
            assertThat(result.getFigi()).isEqualTo(FIGI);
            assertThat(result.getPeriod()).isEqualTo(PERIOD);
            assertThat(result.getEma()).isNotNull();
            assertThat(result.getEma()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getValues()).isNotNull().hasSize(10);
        }

        @Test
        @DisplayName("выбрасывает исключение при недостатке данных")
        void shouldThrowWhenNotEnoughData() {
            List<PriceDataDto> fewPrices = priceData.subList(0, 2);
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, fewPrices, PERIOD);

            assertThatThrownBy(() -> analyticsService.calculateEMA(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Недостаточно данных для расчета EMA");
        }
    }

    @Nested
    @DisplayName("calculateVolatility")
    class CalculateVolatility {

        @Test
        @DisplayName("возвращает корректную волатильность при достаточном количестве данных")
        void shouldReturnVolatilityWhenEnoughData() {
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, priceData, PERIOD);

            VolatilityResponseDto result = analyticsService.calculateVolatility(request);

            assertThat(result).isNotNull();
            assertThat(result.getFigi()).isEqualTo(FIGI);
            assertThat(result.getPeriod()).isEqualTo(PERIOD);
            assertThat(result.getVolatility()).isNotNull();
            assertThat(result.getStandardDeviation()).isNotNull();
            assertThat(result.getStandardDeviation()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("выбрасывает исключение при недостатке данных")
        void shouldThrowWhenNotEnoughData() {
            List<PriceDataDto> fewPrices = priceData.subList(0, 2);
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, fewPrices, PERIOD);

            assertThatThrownBy(() -> analyticsService.calculateVolatility(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Недостаточно данных для расчета волатильности");
        }
    }

    @Nested
    @DisplayName("calculateMACD")
    class CalculateMACD {

        private List<PriceDataDto> priceDataForMacd;

        @BeforeEach
        void initMacdData() {
            LocalDateTime base = LocalDateTime.of(2025, 1, 1, 12, 0);
            priceDataForMacd = IntStream.rangeClosed(1, 30)
                    .mapToObj(i -> new PriceDataDto(FIGI, BigDecimal.valueOf(100 + i), base.plusDays(i)))
                    .toList();
        }

        @Test
        @DisplayName("возвращает корректный MACD при достаточном количестве данных (>= 26)")
        void shouldReturnMacdWhenEnoughData() {
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, priceDataForMacd, PERIOD);

            MacdResponseDto result = analyticsService.calculateMACD(request);

            assertThat(result).isNotNull();
            assertThat(result.getFigi()).isEqualTo(FIGI);
            assertThat(result.getMacd()).isNotNull();
            assertThat(result.getSignal()).isNotNull();
            assertThat(result.getHistogram()).isNotNull();
            assertThat(result.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("выбрасывает исключение при менее 26 точках данных")
        void shouldThrowWhenLessThan26DataPoints() {
            List<PriceDataDto> fewPrices = priceData.subList(0, 10);
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, fewPrices, PERIOD);

            assertThatThrownBy(() -> analyticsService.calculateMACD(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Недостаточно данных для расчета MACD");
        }
    }

    @Nested
    @DisplayName("генерация истории цен (priceData = null/empty)")
    class GeneratePriceHistory {

        @Test
        @DisplayName("SMA: при пустом priceData запрашивает цены и генерирует историю")
        void smaUsesGeneratedHistoryWhenPriceDataEmpty() {
            when(priceServiceClient.getPrices(anyList()))
                    .thenReturn(List.of(new PriceDataDto(FIGI, BigDecimal.valueOf(100), LocalDateTime.now())));
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, null, PERIOD);

            SmaResponseDto result = analyticsService.calculateSMA(request);

            assertThat(result).isNotNull();
            assertThat(result.getFigi()).isEqualTo(FIGI);
            assertThat(result.getSma()).isNotNull();
        }

        @Test
        @DisplayName("при ошибке PriceService используется цена по умолчанию для генерации истории")
        void usesDefaultPriceWhenPriceServiceFails() {
            when(priceServiceClient.getPrices(anyList())).thenThrow(new RuntimeException("Сервис недоступен"));
            AnalyticsRequestDto request = new AnalyticsRequestDto(FIGI, Collections.emptyList(), PERIOD);

            SmaResponseDto result = analyticsService.calculateSMA(request);

            assertThat(result).isNotNull();
            assertThat(result.getSma()).isNotNull();
        }
    }
}
