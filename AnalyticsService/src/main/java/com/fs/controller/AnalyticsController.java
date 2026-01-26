package com.fs.controller;

import com.fs.dto.*;
import com.fs.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics Controller", description = "API для технического анализа")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/sma")
    @Operation(summary = "Вычислить Simple Moving Average")
    public ResponseEntity<SmaResponseDto> calculateSMA(@RequestBody @Valid AnalyticsRequestDto request) {
        log.info("Запрос на расчет SMA для FIGI: {}, период: {}", request.getFigi(), request.getPeriod());
        SmaResponseDto response = analyticsService.calculateSMA(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ema")
    @Operation(summary = "Вычислить Exponential Moving Average")
    public ResponseEntity<EmaResponseDto> calculateEMA(@RequestBody @Valid AnalyticsRequestDto request) {
        log.info("Запрос на расчет EMA для FIGI: {}, период: {}", request.getFigi(), request.getPeriod());
        EmaResponseDto response = analyticsService.calculateEMA(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/volatility")
    @Operation(summary = "Вычислить волатильность")
    public ResponseEntity<VolatilityResponseDto> calculateVolatility(@RequestBody @Valid AnalyticsRequestDto request) {
        log.info("Запрос на расчет волатильности для FIGI: {}, период: {}", request.getFigi(), request.getPeriod());
        VolatilityResponseDto response = analyticsService.calculateVolatility(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/macd")
    @Operation(summary = "Вычислить MACD")
    public ResponseEntity<MacdResponseDto> calculateMACD(@RequestBody @Valid AnalyticsRequestDto request) {
        log.info("Запрос на расчет MACD для FIGI: {}", request.getFigi());
        MacdResponseDto response = analyticsService.calculateMACD(request);
        return ResponseEntity.ok(response);
    }
}
