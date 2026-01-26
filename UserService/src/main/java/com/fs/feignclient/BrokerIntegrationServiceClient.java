package com.fs.feignclient;

import com.fs.dto.StocksDto;
import com.fs.dto.TickersDto;
import com.fs.model.Stock;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "broker-integration-service")
public interface BrokerIntegrationServiceClient {
    
    @GetMapping("/broker/stocks/{ticker}")
    Stock getStock(@PathVariable String ticker);
    
    @PostMapping("/broker/stocks/getStocksByTickers")
    StocksDto getStocksByTickers(TickersDto tickers);
}
