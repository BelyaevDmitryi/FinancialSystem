package com.fs.feignclient;

import com.fs.dto.OrderStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "trading-terminal-service")
public interface TradingTerminalServiceClient {

    @GetMapping("/admin/stats/orders")
    OrderStatsDto getOrderStats();
}
