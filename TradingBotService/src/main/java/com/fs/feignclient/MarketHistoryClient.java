package com.fs.feignclient;

import com.fs.config.FeignClientConfiguration;
import com.fs.dto.HistoricCandlesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@FeignClient(name = "market-history-service", configuration = FeignClientConfiguration.class)
public interface MarketHistoryClient {

    @GetMapping("/market-history/candles")
    HistoricCandlesDto getCandles(
            @RequestParam String figi,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false, defaultValue = "DAY") String interval,
            @RequestParam(required = false) String broker
    );
}
