package com.fs.feignclient;

import com.fs.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "analytics-service")
public interface AnalyticsServiceClient {

    @PostMapping("/analytics/sma")
    SmaResponseDto calculateSMA(@RequestBody AnalyticsRequestDto request);

    @PostMapping("/analytics/ema")
    EmaResponseDto calculateEMA(@RequestBody AnalyticsRequestDto request);

    @PostMapping("/analytics/macd")
    MacdResponseDto calculateMACD(@RequestBody AnalyticsRequestDto request);

    @PostMapping("/analytics/volatility")
    VolatilityResponseDto calculateVolatility(@RequestBody AnalyticsRequestDto request);
}
