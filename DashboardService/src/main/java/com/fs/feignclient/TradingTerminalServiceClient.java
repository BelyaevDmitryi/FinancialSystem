package com.fs.feignclient;

import com.fs.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "trading-terminal-service")
public interface TradingTerminalServiceClient {

    @GetMapping("/orders")
    List<OrderDto> getUserOrders(@RequestHeader("X-User-Id") String userId);
}
