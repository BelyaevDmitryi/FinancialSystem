package com.fs.feignclient;

import com.fs.dto.PriceDataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "price-service")
public interface PriceServiceClient {

    @GetMapping("/prices")
    List<PriceDataDto> getPrices(@RequestParam("figies") List<String> figies);
}
