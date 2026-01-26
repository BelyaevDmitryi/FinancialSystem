package com.fs.feignclient;

import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "trading-terminal-service")
public interface TradingTerminalServiceClient {

    @PostMapping("/orders")
    OrderDto createOrder(@RequestHeader("X-User-Id") String userId, @RequestBody CreateOrderDto createOrderDto);

    @GetMapping("/orders")
    List<OrderDto> getUserOrders(@RequestHeader("X-User-Id") String userId);
}
