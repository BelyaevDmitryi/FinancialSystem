package com.fs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fs.dto.FigiesDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.model.Stock;
import com.fs.service.StockService;

@RestController
@RequiredArgsConstructor
public class StockController {
    private final StockService stockService;

    @GetMapping("/stocks/{ticker}")
    public Stock getStock(@PathVariable String ticker) {
        return stockService.getStockByTicker(ticker);
    }

    @PostMapping("/stocks/getStocksByTickers")
    public StocksDto getStocksByTickers(@RequestBody TickersDto tickersDto) {
        return stockService.getStocksByTickers(tickersDto);
    }

    @PostMapping("/prices")
    public StocksPricesDto getPrices(@RequestBody FigiesDto figiesDto) {
        return stockService.getPrices(figiesDto);
    }
}
