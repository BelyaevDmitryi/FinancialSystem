package com.fs.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class DailyChangeCalculator {

    public DailyChangeResult calculate(
            List<PositionSnapshot> positions,
            Map<String, BigDecimal> currentPrices,
            Map<String, BigDecimal> baselinePrices) {

        BigDecimal currentValue = portfolioValue(positions, currentPrices);
        BigDecimal valueAtPreviousClose = portfolioValue(positions, baselinePrices);

        if (valueAtPreviousClose.compareTo(BigDecimal.ZERO) == 0) {
            return new DailyChangeResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal dailyChange = currentValue.subtract(valueAtPreviousClose);
        BigDecimal dailyChangePercent = dailyChange
                .divide(valueAtPreviousClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return new DailyChangeResult(dailyChange, dailyChangePercent);
    }

    private BigDecimal portfolioValue(List<PositionSnapshot> positions, Map<String, BigDecimal> prices) {
        return positions.stream()
                .map(position -> prices.getOrDefault(position.figi(), BigDecimal.ZERO)
                        .multiply(position.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
