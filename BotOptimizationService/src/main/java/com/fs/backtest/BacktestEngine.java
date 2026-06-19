package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.dto.BrokerCandleDto;
import com.fs.trading.core.SmaCrossoverStrategy;
import com.fs.trading.core.TradeSignal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BacktestEngine {

    private final SmaCrossoverStrategy smaStrategy;
    private final BacktestMetricsCalculator metricsCalculator;

    public BacktestEngine(SmaCrossoverStrategy smaStrategy, BacktestMetricsCalculator metricsCalculator) {
        this.smaStrategy = smaStrategy;
        this.metricsCalculator = metricsCalculator;
    }

    public BacktestResultDto run(List<BrokerCandleDto> candles, BacktestRunRequest request) {
        if (candles == null || candles.isEmpty()) {
            BacktestResultDto empty = new BacktestResultDto();
            empty.setTotalReturn(java.math.BigDecimal.ZERO);
            empty.setMaxDrawdown(java.math.BigDecimal.ZERO);
            empty.setProfitFactor(java.math.BigDecimal.ZERO);
            empty.setFinalEquity(request.getInitialCash());
            empty.setTrades(0);
            return empty;
        }

        SimulatedJournal journal = new SimulatedJournal(request.getInitialCash(), request.getSlippageBps());
        List<EquityPoint> equityCurve = new ArrayList<>();
        int smaPeriod = request.getSmaPeriod();

        for (int i = 0; i < candles.size(); i++) {
            List<BrokerCandleDto> window = candles.subList(0, i + 1);
            BrokerCandleDto bar = candles.get(i);
            TradeSignal signal = smaStrategy.evaluate(
                    BacktestCandles.toCore(window), journal.getPositionQty(), smaPeriod);

            if (signal == TradeSignal.BUY) {
                java.math.BigDecimal qty = journal.maxBuyQuantity(bar.getClose());
                journal.buy(bar, qty);
            } else if (signal == TradeSignal.SELL) {
                journal.sell(bar, journal.getPositionQty());
            }

            equityCurve.add(new EquityPoint(bar.getTime(), journal.equity(bar.getClose())));
        }

        return metricsCalculator.calculate(journal, equityCurve);
    }
}
