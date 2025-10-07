package com.fs.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksWithPrices;

@FeignClient(name = "stockservice", url = "${api.stockConfig.stockService}", configuration = FeignConfig.class)
public interface StockServiceClient {
    @PostMapping("${api.stockConfig.getPrices}")
    StocksWithPrices getPrices(StocksDto stocksDto);
}
