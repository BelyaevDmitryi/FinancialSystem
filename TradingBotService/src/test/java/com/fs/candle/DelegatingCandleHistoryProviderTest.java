package com.fs.candle;

import com.fs.config.MarketHistoryCandleProperties;
import com.fs.dto.PriceDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegatingCandleHistoryProviderTest {

    @Mock
    private MarketHistoryCandleProvider marketHistoryCandleProvider;

    @Mock
    private PriceServiceCandleProvider priceServiceCandleProvider;

    private MarketHistoryCandleProperties properties;
    private DelegatingCandleHistoryProvider provider;

    @BeforeEach
    void setUp() {
        properties = new MarketHistoryCandleProperties();
        provider = new DelegatingCandleHistoryProvider(
                properties, marketHistoryCandleProvider, priceServiceCandleProvider);
    }

    @Test
    void getCandles_whenMarketHistoryDisabled_delegatesToPriceService() {
        properties.setEnabled(false);
        List<PriceDataDto> candles = List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(100), LocalDateTime.now()));
        when(priceServiceCandleProvider.getCandles("BBG004730N88")).thenReturn(candles);

        List<PriceDataDto> result = provider.getCandles("BBG004730N88");

        assertThat(result).isEqualTo(candles);
        verify(priceServiceCandleProvider).getCandles("BBG004730N88");
        verify(marketHistoryCandleProvider, never()).getCandles("BBG004730N88");
    }

    @Test
    void getCandles_whenMarketHistoryEnabledWithData_returnsMarketHistoryCandles() {
        properties.setEnabled(true);
        List<PriceDataDto> candles = List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(110), LocalDateTime.now()));
        when(marketHistoryCandleProvider.getCandles("BBG004730N88")).thenReturn(candles);

        List<PriceDataDto> result = provider.getCandles("BBG004730N88");

        assertThat(result).isEqualTo(candles);
        verify(marketHistoryCandleProvider).getCandles("BBG004730N88");
        verify(priceServiceCandleProvider, never()).getCandles("BBG004730N88");
    }

    @Test
    void getCandles_whenMarketHistoryEnabledButEmpty_fallsBackToPriceService() {
        properties.setEnabled(true);
        List<PriceDataDto> candles = List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(105), LocalDateTime.now()));
        when(marketHistoryCandleProvider.getCandles("BBG004730N88")).thenReturn(List.of());
        when(priceServiceCandleProvider.getCandles("BBG004730N88")).thenReturn(candles);

        List<PriceDataDto> result = provider.getCandles("BBG004730N88");

        assertThat(result).isEqualTo(candles);
        verify(marketHistoryCandleProvider).getCandles("BBG004730N88");
        verify(priceServiceCandleProvider).getCandles("BBG004730N88");
    }
}
