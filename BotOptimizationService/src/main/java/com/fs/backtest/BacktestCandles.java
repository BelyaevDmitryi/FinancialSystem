package com.fs.backtest;

import com.fs.dto.BrokerCandleDto;
import com.fs.trading.core.Candle;

import java.util.List;

final class BacktestCandles {

    private BacktestCandles() {
    }

    static List<Candle> toCore(List<BrokerCandleDto> brokerCandles) {
        return brokerCandles.stream()
                .map(c -> new Candle(c.getClose()))
                .toList();
    }
}
