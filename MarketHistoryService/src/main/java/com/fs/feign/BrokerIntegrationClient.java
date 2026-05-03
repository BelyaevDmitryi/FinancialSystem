package com.fs.feign;

import com.fs.dto.HistoricCandlesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@FeignClient(name = "broker-integration-service")
public interface BrokerIntegrationClient {

    @GetMapping("/broker/history/candles")
    HistoricCandlesDto getHistoricCandles(
            @RequestParam String figi,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false, defaultValue = "DAY") String interval,
            @RequestParam(required = false) String broker
    );
}
