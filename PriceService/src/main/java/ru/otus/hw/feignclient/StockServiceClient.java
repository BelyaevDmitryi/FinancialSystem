package ru.otus.hw.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksWithPrices;

@FeignClient(name = "stockservice", url = "${api.stockConfig.stockService}", configuration = FeignConfig.class)
public interface StockServiceClient {
    @PostMapping("${api.stockConfig.getPrices}")
    StocksWithPrices getPrices(StocksDto stocksDto);
}
