package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.feign.MarketHistoryClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BacktestService {

    private final MarketHistoryClient marketHistoryClient;
    private final BacktestEngine backtestEngine;

    public BacktestService(MarketHistoryClient marketHistoryClient, BacktestEngine backtestEngine) {
        this.marketHistoryClient = marketHistoryClient;
        this.backtestEngine = backtestEngine;
    }

    public BacktestResultDto run(BacktestRunRequest request) {
        HistoricCandlesDto history = marketHistoryClient.getCandles(
                request.getFigi(),
                request.getFrom(),
                request.getTo(),
                request.getInterval(),
                null);
        List<BrokerCandleDto> candles = history.getCandles() != null ? history.getCandles() : List.of();
        return backtestEngine.run(candles, request);
    }

    /**
     * Offline run for unit tests (no Feign).
     */
    public BacktestResultDto runOnCandles(List<BrokerCandleDto> candles, BacktestRunRequest request) {
        return backtestEngine.run(candles, request);
    }
}
