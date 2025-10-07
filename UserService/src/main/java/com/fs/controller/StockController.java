package com.fs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.fs.domain.Stock;
import com.fs.service.StockService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {
    private final StockService stockService;

    @GetMapping("/{ticker}")
    public Stock getStockByTicker(@PathVariable String ticker) {
        Stock stock = stockService.getStockByTicker(ticker);
        return stock;
    }

    @DeleteMapping("/{ticker}")
    public void deleteStockByTicker(@PathVariable String ticker) {
        stockService.deleteStockByTicker(ticker);
    }

    @GetMapping
    public List<Stock> getAllStocks() {
        log.info("Getting all stocks");
        return stockService.getAllStocks();
    }
}
