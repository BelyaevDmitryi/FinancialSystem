package com.fs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.fs.domain.Stock;
import com.fs.dto.FigisDto;
import com.fs.dto.StockMetadataDto;
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

    @PostMapping("/by-figis")
    public List<StockMetadataDto> getStocksByFigis(@RequestBody FigisDto figisDto) {
        return stockService.getStocksByFigis(figisDto.figis()).stream()
                .map(stock -> new StockMetadataDto(
                        stock.getFigi(),
                        stock.getTicker(),
                        stock.getName(),
                        stock.getCurrency().name()))
                .toList();
    }
}
