package com.fs.candle;

import com.fs.config.BotProperties;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.PriceServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceServiceCandleProvider implements CandleHistoryProvider {

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);

    private final PriceServiceClient priceServiceClient;
    private final BotProperties botProperties;

    @Override
    public List<PriceDataDto> getCandles(String figi) {
        int lookback = botProperties.getCandleLookback();
        List<PriceDataDto> prices = priceServiceClient.getPrices(List.of(figi));
        if (prices == null || prices.isEmpty()) {
            log.warn("PriceService вернул пустой список свечей для FIGI: {}", figi);
            return List.of();
        }

        List<PriceDataDto> sorted = prices.stream()
                .sorted(Comparator.comparing(PriceDataDto::getTimestamp))
                .toList();

        if (sorted.size() >= lookback) {
            return sorted.subList(sorted.size() - lookback, sorted.size());
        }
        if (sorted.size() > 1) {
            return sorted;
        }

        return synthesizeLookback(figi, sorted.get(0).getPrice(), lookback);
    }

    private List<PriceDataDto> synthesizeLookback(String figi, BigDecimal basePrice, int lookback) {
        List<PriceDataDto> candles = new ArrayList<>(lookback);
        LocalDateTime now = LocalDateTime.now();
        for (int i = lookback - 1; i >= 0; i--) {
            double variation = ((i % 5) - 2) * 0.01;
            BigDecimal price = basePrice.multiply(BigDecimal.valueOf(1.0 + variation), MATH_CONTEXT);
            candles.add(new PriceDataDto(figi, price, now.minusMinutes(i)));
        }
        log.debug("Синтезировано {} свечей для FIGI {} из текущей цены {}", lookback, figi, basePrice);
        return candles;
    }
}
