package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class BacktestMetricsCalculator {

    private static final int SCALE = 8;

    public BacktestResultDto calculate(SimulatedJournal journal, List<EquityPoint> equityCurve) {
        BacktestResultDto result = new BacktestResultDto();
        result.setTrades(journal.getTrades().size());
        result.setEquityCurve(equityCurve);

        BigDecimal initial = journal.getInitialCash();
        BigDecimal finalEquity = equityCurve.isEmpty()
                ? initial
                : equityCurve.get(equityCurve.size() - 1).equity();

        if (initial.signum() > 0) {
            BigDecimal totalReturn = finalEquity.subtract(initial)
                    .divide(initial, SCALE, RoundingMode.HALF_UP);
            result.setTotalReturn(totalReturn);
        } else {
            result.setTotalReturn(BigDecimal.ZERO);
        }

        result.setMaxDrawdown(computeMaxDrawdown(equityCurve));
        result.setProfitFactor(computeProfitFactor(journal));
        result.setFinalEquity(finalEquity);
        return result;
    }

    private static BigDecimal computeMaxDrawdown(List<EquityPoint> equityCurve) {
        if (equityCurve.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal peak = equityCurve.get(0).equity();
        BigDecimal maxDd = BigDecimal.ZERO;
        for (EquityPoint point : equityCurve) {
            BigDecimal equity = point.equity();
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            if (peak.signum() > 0) {
                BigDecimal dd = peak.subtract(equity).divide(peak, SCALE, RoundingMode.HALF_UP);
                if (dd.compareTo(maxDd) > 0) {
                    maxDd = dd;
                }
            }
        }
        return maxDd;
    }

    private static BigDecimal computeProfitFactor(SimulatedJournal journal) {
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        for (SimulatedTrade trade : journal.getTrades()) {
            if (trade.realizedPnl() == null) {
                continue;
            }
            if (trade.realizedPnl().signum() > 0) {
                grossProfit = grossProfit.add(trade.realizedPnl());
            } else {
                grossLoss = grossLoss.add(trade.realizedPnl().abs());
            }
        }
        if (grossLoss.signum() == 0) {
            return grossProfit.signum() > 0 ? BigDecimal.valueOf(999) : BigDecimal.ZERO;
        }
        return grossProfit.divide(grossLoss, SCALE, RoundingMode.HALF_UP);
    }
}
