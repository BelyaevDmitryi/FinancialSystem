package com.fs.service;

import com.fs.backtest.BacktestEngine;
import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.GridOptimizationRequest;
import com.fs.dto.GridOptimizationResponseDto;
import com.fs.dto.GridOptimizationRunResultDto;
import com.fs.dto.GridParameterSpecDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.feign.MarketHistoryClient;
import com.fs.optimization.GridParameterGenerator;
import com.fs.optimization.OptimizationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class MultiParamOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(MultiParamOptimizationService.class);

    private final MarketHistoryClient marketHistoryClient;
    private final BacktestEngine backtestEngine;
    private final GridParameterGenerator gridParameterGenerator;
    private final OptimizationFilter optimizationFilter;

    public MultiParamOptimizationService(
            MarketHistoryClient marketHistoryClient,
            BacktestEngine backtestEngine,
            GridParameterGenerator gridParameterGenerator,
            OptimizationFilter optimizationFilter) {
        this.marketHistoryClient = marketHistoryClient;
        this.backtestEngine = backtestEngine;
        this.gridParameterGenerator = gridParameterGenerator;
        this.optimizationFilter = optimizationFilter;
    }

    public GridOptimizationResponseDto optimize(GridOptimizationRequest request) {
        validateRequest(request);

        String interval = request.getInterval() != null ? request.getInterval() : "DAY";
        HistoricCandlesDto history = marketHistoryClient.getCandles(
                request.getFigi(),
                request.getFrom(),
                request.getTo(),
                interval,
                null);
        List<BrokerCandleDto> candles = history.getCandles() != null ? history.getCandles() : List.of();
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("Нет свечей за выбранный период");
        }

        List<Map<String, Double>> combinations = buildParameterCombinations(request.getParameters());
        int poolSize = Math.max(1, Math.min(request.getParallelPoolSize(), combinations.size()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Future<GridOptimizationRunResultDto>> futures = new ArrayList<>();
            for (Map<String, Double> params : combinations) {
                futures.add(executor.submit(() -> runBacktest(request, candles, params)));
            }

            List<GridOptimizationRunResultDto> allRuns = new ArrayList<>();
            for (Future<GridOptimizationRunResultDto> future : futures) {
                allRuns.add(future.get());
            }

            List<GridOptimizationRunResultDto> filtered = allRuns.stream()
                    .filter(run -> optimizationFilter.accepts(toBacktestResult(run), request.getFilters()))
                    .sorted(Comparator.comparing(GridOptimizationRunResultDto::getTotalReturn,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toCollection(ArrayList::new));

            for (int i = 0; i < filtered.size(); i++) {
                filtered.get(i).setRank(i + 1);
            }

            GridOptimizationResponseDto response = new GridOptimizationResponseDto();
            response.setFigi(request.getFigi());
            response.setTotalRuns(combinations.size());
            response.setPassedFilters(filtered.size());
            response.setResults(filtered);
            log.info("Grid optimization figi={}: {} runs, {} passed filters",
                    request.getFigi(), combinations.size(), filtered.size());
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ошибка grid-оптимизации", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Ошибка grid-оптимизации", e.getCause());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Offline optimization for unit tests (no Feign).
     */
    public GridOptimizationResponseDto optimizeOnCandles(
            GridOptimizationRequest request, List<BrokerCandleDto> candles) {
        validateRequest(request);
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Нет свечей за выбранный период");
        }

        List<Map<String, Double>> combinations = buildParameterCombinations(request.getParameters());
        List<GridOptimizationRunResultDto> allRuns = combinations.stream()
                .map(params -> runBacktest(request, candles, params))
                .toList();

        List<GridOptimizationRunResultDto> filtered = allRuns.stream()
                .filter(run -> optimizationFilter.accepts(toBacktestResult(run), request.getFilters()))
                .sorted(Comparator.comparing(GridOptimizationRunResultDto::getTotalReturn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRank(i + 1);
        }

        GridOptimizationResponseDto response = new GridOptimizationResponseDto();
        response.setFigi(request.getFigi());
        response.setTotalRuns(combinations.size());
        response.setPassedFilters(filtered.size());
        response.setResults(filtered);
        return response;
    }

    private GridOptimizationRunResultDto runBacktest(
            GridOptimizationRequest request, List<BrokerCandleDto> candles, Map<String, Double> params) {
        BacktestRunRequest backtestRequest = new BacktestRunRequest();
        backtestRequest.setFigi(request.getFigi());
        backtestRequest.setFrom(request.getFrom());
        backtestRequest.setTo(request.getTo());
        backtestRequest.setInterval(request.getInterval());
        backtestRequest.setInitialCash(request.getInitialCash());
        backtestRequest.setSlippageBps(request.getSlippageBps());
        applyParameters(backtestRequest, params);

        BacktestResultDto metrics = backtestEngine.run(candles, backtestRequest);
        GridOptimizationRunResultDto run = new GridOptimizationRunResultDto();
        run.setParameters(new LinkedHashMap<>(params));
        run.setTotalReturn(metrics.getTotalReturn());
        run.setMaxDrawdown(metrics.getMaxDrawdown());
        run.setProfitFactor(metrics.getProfitFactor());
        run.setFinalEquity(metrics.getFinalEquity());
        run.setTrades(metrics.getTrades());
        return run;
    }

    private void applyParameters(BacktestRunRequest backtestRequest, Map<String, Double> params) {
        Double smaPeriod = params.get("smaPeriod");
        if (smaPeriod != null) {
            backtestRequest.setSmaPeriod(smaPeriod.intValue());
        }
    }

    private List<Map<String, Double>> buildParameterCombinations(List<GridParameterSpecDto> specs) {
        List<Map<String, Double>> combinations = new ArrayList<>();
        combinations.add(new LinkedHashMap<>());
        for (GridParameterSpecDto spec : specs) {
            List<Double> values = gridParameterGenerator.generate(
                    spec.getMin(), spec.getMax(), spec.getStep(), spec.getStepType());
            List<Map<String, Double>> expanded = new ArrayList<>();
            for (Map<String, Double> existing : combinations) {
                for (Double value : values) {
                    Map<String, Double> combo = new LinkedHashMap<>(existing);
                    combo.put(spec.getName(), value);
                    expanded.add(combo);
                }
            }
            combinations = expanded;
        }
        return combinations;
    }

    private static BacktestResultDto toBacktestResult(GridOptimizationRunResultDto run) {
        BacktestResultDto dto = new BacktestResultDto();
        dto.setTotalReturn(run.getTotalReturn());
        dto.setMaxDrawdown(run.getMaxDrawdown());
        dto.setProfitFactor(run.getProfitFactor());
        dto.setFinalEquity(run.getFinalEquity());
        dto.setTrades(run.getTrades());
        return dto;
    }

    private static void validateRequest(GridOptimizationRequest request) {
        if (request.getFrom().isAfter(request.getTo())) {
            throw new IllegalArgumentException("from должен быть не позже to");
        }
        if (request.getParameters() == null || request.getParameters().isEmpty()) {
            throw new IllegalArgumentException("parameters не может быть пустым");
        }
    }
}
