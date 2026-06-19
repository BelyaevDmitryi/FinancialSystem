package com.fs.controller;

import com.fs.dto.GridOptimizationRequest;
import com.fs.dto.GridOptimizationResponseDto;
import com.fs.dto.OptimizationResultDto;
import com.fs.dto.SmaGridOptimizationRequest;
import com.fs.service.MultiParamOptimizationService;
import com.fs.service.SmaTrendOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bot-optimization")
@Tag(name = "Bot optimization", description = "Подбор параметров по историческим данным")
public class OptimizationController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationController.class);

    private final SmaTrendOptimizationService smaTrendOptimizationService;
    private final MultiParamOptimizationService multiParamOptimizationService;

    public OptimizationController(
            SmaTrendOptimizationService smaTrendOptimizationService,
            MultiParamOptimizationService multiParamOptimizationService) {
        this.smaTrendOptimizationService = smaTrendOptimizationService;
        this.multiParamOptimizationService = multiParamOptimizationService;
    }

    @PostMapping("/sma-trend-grid")
    @Operation(summary = "Перебор периода SMA: максимум доли баров с close > SMA(period)")
    public ResponseEntity<OptimizationResultDto> optimizeSmaTrend(
            @Valid @RequestBody SmaGridOptimizationRequest request) {
        log.info("Оптимизация SMA по figi={} {}..{}", request.getFigi(), request.getFrom(), request.getTo());
        return ResponseEntity.ok(smaTrendOptimizationService.optimize(request));
    }

    @PostMapping("/grid")
    @Operation(summary = "Многопараметрический grid: BacktestEngine in-process, фильтры и ранжирование")
    public ResponseEntity<GridOptimizationResponseDto> optimizeGrid(
            @Valid @RequestBody GridOptimizationRequest request) {
        log.info("Grid-оптимизация figi={} {}..{} params={}",
                request.getFigi(), request.getFrom(), request.getTo(), request.getParameters().size());
        return ResponseEntity.ok(multiParamOptimizationService.optimize(request));
    }
}
