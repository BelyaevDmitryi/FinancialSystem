package com.fs.controller;

import com.fs.dto.HistoricCandlesDto;
import com.fs.service.MarketHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/market-history")
@Tag(name = "Market History", description = "Загрузка исторических рыночных данных через брокера")
public class MarketHistoryController {

    private static final Logger log = LoggerFactory.getLogger(MarketHistoryController.class);

    private final MarketHistoryService marketHistoryService;

    public MarketHistoryController(MarketHistoryService marketHistoryService) {
        this.marketHistoryService = marketHistoryService;
    }

    @GetMapping("/candles")
    @Operation(summary = "Исторические свечи по FIGI (UTC), прокси к BrokerIntegrationService")
    public ResponseEntity<HistoricCandlesDto> getCandles(
            @RequestParam String figi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "DAY") String interval,
            @RequestParam(required = false) String broker) {
        log.debug("GET candles figi={}", figi);
        HistoricCandlesDto dto = marketHistoryService.fetchCandles(figi, from, to, interval, broker);
        return ResponseEntity.ok(dto);
    }
}
