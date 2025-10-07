package ru.otus.hw.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.domain.Stock;
import ru.otus.hw.service.StockService;

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
