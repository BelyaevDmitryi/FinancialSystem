package com.fs.feignclient;

import com.fs.dto.FigiesDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;

public interface ApiStockService {
    StocksPricesDto getPrices(FigiesDto figiesDto);
    StocksDto getStocksByTickers(TickersDto tickersDto);
}
