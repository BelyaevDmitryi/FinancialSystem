package ru.otus.hw.feignclient;

import ru.otus.hw.dto.FigiesDto;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksPricesDto;
import ru.otus.hw.dto.TickersDto;

public interface ApiStockService {
    StocksPricesDto getPrices(FigiesDto figiesDto);
    StocksDto getStocksByTickers(TickersDto tickersDto);
}
