package com.fs.service;

import com.fs.backtest.BacktestEngine;
import com.fs.backtest.BacktestMetricsCalculator;
import com.fs.trading.core.SmaCrossoverStrategy;
import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.GridOptimizationFiltersDto;
import com.fs.dto.GridOptimizationRequest;
import com.fs.dto.GridOptimizationResponseDto;
import com.fs.dto.GridParameterSpecDto;
import com.fs.optimization.GridParameterGenerator;
import com.fs.optimization.OptimizationFilter;
import com.fs.optimization.StepType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiParamOptimizationServiceTest {

    private static final String FIGI = "BBG004730N88";

    @Mock
    private BacktestEngine backtestEngine;

    private MultiParamOptimizationService service;
    private List<BrokerCandleDto> candles;

    @BeforeEach
    void setUp() {
        service = new MultiParamOptimizationService(
                null,
                backtestEngine,
                new GridParameterGenerator(),
                new OptimizationFilter());
        candles = buildFlatCandles(30);
    }

    @Test
    void optimizeOnCandles_sixPeriodCombos_invokesBacktestEngineSixTimes() {
        GridOptimizationRequest request = baseRequest();
        GridParameterSpecDto period = new GridParameterSpecDto();
        period.setName("smaPeriod");
        period.setMin(10);
        period.setMax(20);
        period.setStep(2);
        period.setStepType(StepType.ABSOLUTE);
        request.setParameters(List.of(period));

        when(backtestEngine.run(eq(candles), any())).thenAnswer(invocation -> {
            BacktestRunRequest runRequest = invocation.getArgument(1);
            BacktestResultDto result = new BacktestResultDto();
            result.setTotalReturn(BigDecimal.valueOf(runRequest.getSmaPeriod() * 0.001));
            result.setProfitFactor(BigDecimal.valueOf(1.2));
            result.setMaxDrawdown(BigDecimal.valueOf(0.1));
            result.setTrades(4);
            result.setFinalEquity(BigDecimal.valueOf(101_000));
            return result;
        });

        GridOptimizationResponseDto response = service.optimizeOnCandles(request, candles);

        assertThat(response.getTotalRuns()).isEqualTo(6);
        assertThat(response.getPassedFilters()).isEqualTo(6);
        assertThat(response.getResults()).hasSize(6);
        assertThat(response.getResults().get(0).getRank()).isEqualTo(1);
        assertThat(response.getResults().get(0).getParameters().get("smaPeriod")).isEqualTo(20.0);

        verify(backtestEngine, times(6)).run(eq(candles), any());
    }

    @Test
    void optimizeOnCandles_maxDrawdownFilter_excludesHighDrawdownRuns() {
        GridOptimizationRequest request = baseRequest();
        GridParameterSpecDto period = new GridParameterSpecDto();
        period.setName("smaPeriod");
        period.setMin(10);
        period.setMax(12);
        period.setStep(2);
        period.setStepType(StepType.ABSOLUTE);
        request.setParameters(List.of(period));

        GridOptimizationFiltersDto filters = new GridOptimizationFiltersDto();
        filters.setMaxDrawdown(BigDecimal.valueOf(0.15));
        request.setFilters(filters);

        when(backtestEngine.run(eq(candles), any())).thenAnswer(invocation -> {
            BacktestRunRequest runRequest = invocation.getArgument(1);
            BacktestResultDto result = new BacktestResultDto();
            result.setTotalReturn(BigDecimal.ONE);
            result.setProfitFactor(BigDecimal.ONE);
            result.setTrades(3);
            if (runRequest.getSmaPeriod() == 10) {
                result.setMaxDrawdown(BigDecimal.valueOf(0.25));
            } else {
                result.setMaxDrawdown(BigDecimal.valueOf(0.05));
            }
            return result;
        });

        GridOptimizationResponseDto response = service.optimizeOnCandles(request, candles);

        assertThat(response.getTotalRuns()).isEqualTo(2);
        assertThat(response.getPassedFilters()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getParameters().get("smaPeriod")).isEqualTo(12.0);
    }

    @Test
    void optimizeOnCandles_passesSmaPeriodToBacktestRequest() {
        GridOptimizationRequest request = baseRequest();
        GridParameterSpecDto period = new GridParameterSpecDto();
        period.setName("smaPeriod");
        period.setMin(14);
        period.setMax(14);
        period.setStep(1);
        period.setStepType(StepType.ABSOLUTE);
        request.setParameters(List.of(period));

        when(backtestEngine.run(eq(candles), any())).thenReturn(sampleMetrics());

        service.optimizeOnCandles(request, candles);

        ArgumentCaptor<BacktestRunRequest> captor = ArgumentCaptor.forClass(BacktestRunRequest.class);
        verify(backtestEngine).run(eq(candles), captor.capture());
        assertThat(captor.getValue().getSmaPeriod()).isEqualTo(14);
    }

    @Test
    void optimizeOnCandles_integrationWithRealEngine_producesRankedResults() {
        BacktestEngine realEngine = new BacktestEngine(new SmaCrossoverStrategy(), new BacktestMetricsCalculator());
        MultiParamOptimizationService realService = new MultiParamOptimizationService(
                null,
                realEngine,
                new GridParameterGenerator(),
                new OptimizationFilter());

        GridOptimizationRequest request = baseRequest();
        GridParameterSpecDto period = new GridParameterSpecDto();
        period.setName("smaPeriod");
        period.setMin(5);
        period.setMax(7);
        period.setStep(1);
        period.setStepType(StepType.ABSOLUTE);
        request.setParameters(List.of(period));

        GridOptimizationResponseDto response = realService.optimizeOnCandles(request, buildTrendCandles(40));

        assertThat(response.getTotalRuns()).isEqualTo(3);
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getResults().get(0).getTotalReturn()).isNotNull();
    }

    private static GridOptimizationRequest baseRequest() {
        GridOptimizationRequest request = new GridOptimizationRequest();
        request.setFigi(FIGI);
        request.setFrom(Instant.parse("2026-01-01T00:00:00Z"));
        request.setTo(Instant.parse("2026-03-01T00:00:00Z"));
        request.setInitialCash(BigDecimal.valueOf(100_000));
        return request;
    }

    private static BacktestResultDto sampleMetrics() {
        BacktestResultDto result = new BacktestResultDto();
        result.setTotalReturn(BigDecimal.valueOf(0.05));
        result.setProfitFactor(BigDecimal.ONE);
        result.setMaxDrawdown(BigDecimal.valueOf(0.1));
        result.setTrades(2);
        return result;
    }

    private static List<BrokerCandleDto> buildFlatCandles(int count) {
        List<BrokerCandleDto> candles = new ArrayList<>();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            BrokerCandleDto c = new BrokerCandleDto();
            c.setTime(t.plus(i, ChronoUnit.DAYS));
            BigDecimal price = BigDecimal.valueOf(100 + (i % 3));
            c.setOpen(price);
            c.setHigh(price);
            c.setLow(price);
            c.setClose(price);
            c.setVolume(1_000);
            candles.add(c);
        }
        return candles;
    }

    private static List<BrokerCandleDto> buildTrendCandles(int count) {
        List<BrokerCandleDto> candles = new ArrayList<>();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            BrokerCandleDto c = new BrokerCandleDto();
            c.setTime(t.plus(i, ChronoUnit.DAYS));
            BigDecimal price = BigDecimal.valueOf(100 + i);
            c.setOpen(price);
            c.setHigh(price);
            c.setLow(price);
            c.setClose(price);
            c.setVolume(1_000);
            candles.add(c);
        }
        return candles;
    }
}
