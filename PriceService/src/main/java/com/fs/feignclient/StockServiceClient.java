package com.fs.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import com.fs.dto.FigiesDto;
import com.fs.dto.StocksPricesDto;

@FeignClient(name = "broker-integration-service", configuration = FeignConfig.class)
public interface StockServiceClient {
    @PostMapping("/broker/prices")
    StocksPricesDto getPrices(FigiesDto figiesDto);
}
