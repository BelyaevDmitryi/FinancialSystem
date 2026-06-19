package com.fs.candle;

import com.fs.dto.PriceDataDto;

import java.util.List;

public interface CandleHistoryProvider {

    List<PriceDataDto> getCandles(String figi);
}
