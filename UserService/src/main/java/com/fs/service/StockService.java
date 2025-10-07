package com.fs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fs.domain.Stock;
import com.fs.exception.StockNotFoundException;
import com.fs.repository.StockRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    public Stock getStockByTicker(String ticker) {
        return stockRepository.findByTicker(ticker).orElseThrow(() -> new StockNotFoundException("Stock not found. Try another ticker."));
    }

    public void deleteStockByTicker(String ticker) {
        stockRepository.deleteStockByTicker(ticker).orElseThrow(() -> new StockNotFoundException("Stock not found."));
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public List<Stock> getStocksByTickers(List<String> tickers) {
        return stockRepository.findByTickerIn(tickers);
    }
}
