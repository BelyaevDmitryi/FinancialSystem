package com.fs.feignclient;

import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign клиент для интеграции с BrokerIntegrationService
 */
@FeignClient(name = "broker-integration-service")
public interface BrokerIntegrationServiceClient {
    
    @PostMapping("/broker/orders")
    BrokerOrderDto placeOrder(
            @RequestHeader("X-Account-Id") String accountId,
            @RequestBody CreateBrokerOrderDto createOrderDto,
            @RequestParam(required = false) String broker
    );
    
    @PatchMapping("/broker/orders/{orderId}")
    BrokerOrderDto amendOrder(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String orderId,
            @RequestBody AmendBrokerOrderDto amendDto,
            @RequestParam(required = false) String broker
    );

    @PostMapping("/broker/orders/{orderId}/cancel")
    void cancelOrder(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String orderId,
            @RequestParam(required = false) String broker
    );
    
    @GetMapping("/broker/orders/{orderId}")
    BrokerOrderDto getOrderStatus(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String orderId,
            @RequestParam(required = false) String broker
    );
    
    @GetMapping("/broker/orders")
    List<BrokerOrderDto> getActiveOrders(
            @RequestHeader("X-Account-Id") String accountId,
            @RequestParam(required = false) String broker
    );
}
