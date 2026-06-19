package com.fs.strategy;

import com.fs.config.BotProperties;
import com.fs.candle.PriceServiceCandleProvider;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.PriceServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultStrategyContextTest {

    @Mock
    private PriceServiceClient priceServiceClient;

    private PriceServiceCandleProvider candleProvider;

    @BeforeEach
    void setUp() {
        BotProperties botProperties = new BotProperties();
        botProperties.setCandleLookback(30);
        candleProvider = new PriceServiceCandleProvider(priceServiceClient, botProperties);
    }

    @Test
    void getCandles_returnsMockedPriceHistory() {
        List<PriceDataDto> prices = buildPriceSeries(30, BigDecimal.valueOf(100));
        when(priceServiceClient.getPrices(List.of("BBG004730N88"))).thenReturn(prices);

        List<PriceDataDto> candles = candleProvider.getCandles("BBG004730N88");

        assertThat(candles).hasSize(30);
        assertThat(candles.get(29).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(129));
    }

    @Test
    void context_exposesCandlesPositionAndCurrentPrice() {
        List<PriceDataDto> candles = buildPriceSeries(20, BigDecimal.valueOf(110));
        BigDecimal positionQty = BigDecimal.valueOf(5);

        DefaultStrategyContext context = new DefaultStrategyContext(candles, positionQty);

        assertThat(context.getCandles()).hasSize(20);
        assertThat(context.getPositionQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(context.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(129));
    }

    @Test
    void getCandles_synthesizesLookbackFromSinglePricePoint() {
        when(priceServiceClient.getPrices(List.of("BBG004730N88")))
                .thenReturn(List.of(new PriceDataDto("BBG004730N88", BigDecimal.valueOf(250),
                        LocalDateTime.now())));

        List<PriceDataDto> candles = candleProvider.getCandles("BBG004730N88");

        assertThat(candles).hasSize(30);
        assertThat(candles.get(0).getFigi()).isEqualTo("BBG004730N88");
    }

    private static List<PriceDataDto> buildPriceSeries(int count, BigDecimal startPrice) {
        List<PriceDataDto> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            prices.add(new PriceDataDto(
                    "BBG004730N88",
                    startPrice.add(BigDecimal.valueOf(i)),
                    LocalDateTime.now().minusMinutes(count - i)));
        }
        return prices;
    }
}
