package com.fs.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DailyChangeCalculatorTest {

    private final DailyChangeCalculator calculator = new DailyChangeCalculator();

    @Test
    void calculate_returnsDailyChangeAndPercent() {
        List<PositionSnapshot> positions = List.of(
                new PositionSnapshot("FIGI1", BigDecimal.TEN)
        );
        Map<String, BigDecimal> currentPrices = Map.of("FIGI1", BigDecimal.valueOf(110));
        Map<String, BigDecimal> baselinePrices = Map.of("FIGI1", BigDecimal.valueOf(100));

        DailyChangeResult result = calculator.calculate(positions, currentPrices, baselinePrices);

        assertThat(result.dailyChange()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.dailyChangePercent()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void calculate_whenBaselineValueIsZero_returnsZeros() {
        DailyChangeResult result = calculator.calculate(
                List.of(new PositionSnapshot("FIGI1", BigDecimal.ONE)),
                Map.of("FIGI1", BigDecimal.TEN),
                Map.of()
        );

        assertThat(result.dailyChange()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.dailyChangePercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
