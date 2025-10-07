package ru.otus.hw.service;

import ru.otus.hw.dto.FigiesDto;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksPricesDto;
import ru.otus.hw.dto.TickersDto;
import ru.otus.hw.model.Stock;

import java.util.concurrent.ExecutionException;

public interface StockService {

    Stock getStockByTicker(String ticker);
    StocksDto getStocksByTickers(TickersDto tickersDto);
    StocksPricesDto getPrices(FigiesDto figiesDto);
}
