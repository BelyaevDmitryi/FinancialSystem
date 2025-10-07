package com.fs.service;

import com.fs.dto.FigiesDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.model.Stock;

public interface StockService {

    Stock getStockByTicker(String ticker);
    StocksDto getStocksByTickers(TickersDto tickersDto);
    StocksPricesDto getPrices(FigiesDto figiesDto);
}
