package com.fs.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import com.fs.dto.FigiesDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;

@FeignClient(name = "tinkoffservice", url = "${api.tinkoffConfig.tinkoffService}", configuration = FeignConfig.class)
public interface TinkoffServiceClient extends ApiStockService {
    @PostMapping("${api.tinkoffConfig.getStocksByTickers}")
    StocksDto getStocksByTickers(TickersDto tickersDto);

    @PostMapping("${api.tinkoffConfig.getPrices}")
    StocksPricesDto getPrices(FigiesDto figiesDto);
}
