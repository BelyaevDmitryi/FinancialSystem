package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
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
@RequestMapping("/backtest")
@Tag(name = "Backtest", description = "Симуляция стратегии на исторических свечах")
public class BacktestController {

    private static final Logger log = LoggerFactory.getLogger(BacktestController.class);

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @PostMapping("/run")
    @Operation(summary = "Запуск backtest SMA на свечах из MarketHistory")
    public ResponseEntity<BacktestResultDto> run(@Valid @RequestBody BacktestRunRequest request) {
        log.info("Backtest figi={} {}..{} smaPeriod={}", request.getFigi(), request.getFrom(), request.getTo(),
                request.getSmaPeriod());
        return ResponseEntity.ok(backtestService.run(request));
    }
}
