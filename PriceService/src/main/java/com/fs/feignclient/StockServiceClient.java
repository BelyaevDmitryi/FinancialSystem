package com.fs.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.fs.dto.FigiesDto;
import com.fs.dto.StocksPricesDto;

@FeignClient(name = "broker-integration-service", configuration = FeignConfig.class)
public interface StockServiceClient {
    @PostMapping("/broker/prices")
    StocksPricesDto getPrices(@RequestBody FigiesDto figiesDto);

    @PostMapping("/broker/prices")
    StocksPricesDto getPrices(@RequestBody FigiesDto figiesDto, @RequestParam("broker") String broker);
}
