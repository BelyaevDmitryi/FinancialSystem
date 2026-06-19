package com.fs.strategy;

import com.fs.dto.PriceDataDto;
import com.fs.trading.core.Candle;

import java.util.List;

final class StrategyCandleMapper {

    private StrategyCandleMapper() {
    }

    static List<Candle> fromPriceData(List<PriceDataDto> prices) {
        return prices.stream()
                .map(p -> new Candle(p.getPrice()))
                .toList();
    }
}
