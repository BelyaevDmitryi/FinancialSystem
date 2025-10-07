package com.fs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksWithPrices;
import com.fs.dto.TickersDto;
import com.fs.service.StockService;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {
    private final StockService stockService;

    @PostMapping("/getByTickers")
    public StocksDto getStocksByTickers(@RequestBody TickersDto tickersDto) {
        return stockService.getStocksByTickers(tickersDto);
    }

    @PostMapping("/prices")
    public StocksWithPrices getPrices(@RequestBody StocksDto stocksDto) {
        return stockService.getPrices(stocksDto);
    }
}
