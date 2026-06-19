package com.fs.controller;

import com.fs.dto.CandleImportRequest;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.ImportJobStatusDto;
import com.fs.service.CandleImportService;
import com.fs.service.MarketHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/market-history")
@Tag(name = "Market History", description = "Исторические свечи: БД, импорт и прокси к брокеру")
public class MarketHistoryController {

    private static final Logger log = LoggerFactory.getLogger(MarketHistoryController.class);

    private final MarketHistoryService marketHistoryService;
    private final CandleImportService candleImportService;

    public MarketHistoryController(
            MarketHistoryService marketHistoryService,
            CandleImportService candleImportService) {
        this.marketHistoryService = marketHistoryService;
        this.candleImportService = candleImportService;
    }

    @GetMapping("/candles")
    @Operation(summary = "Исторические свечи по FIGI (UTC): сначала БД, иначе брокер")
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

    @PostMapping("/import")
    @Operation(summary = "Запуск асинхронного импорта свечей из брокера в БД")
    public ResponseEntity<Map<String, Long>> startImport(@Valid @RequestBody CandleImportRequest request) {
        log.info("POST import figi={} {}..{}", request.getFigi(), request.getFrom(), request.getTo());
        Long jobId = candleImportService.startImport(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    @GetMapping("/import/{jobId}")
    @Operation(summary = "Статус задачи импорта")
    public ResponseEntity<ImportJobStatusDto> getImportStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(candleImportService.getJobStatus(jobId));
    }
}
