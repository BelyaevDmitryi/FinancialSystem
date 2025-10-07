package ru.otus.hw.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import ru.otus.hw.dto.FigiesDto;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksPricesDto;
import ru.otus.hw.dto.TickersDto;

@FeignClient(name = "tinkoffservice", url = "${api.tinkoffConfig.tinkoffService}", configuration = FeignConfig.class)
public interface TinkoffServiceClient extends ApiStockService {
    @PostMapping("${api.tinkoffConfig.getStocksByTickers}")
    StocksDto getStocksByTickers(TickersDto tickersDto);

    @PostMapping("${api.tinkoffConfig.getPrices}")
    StocksPricesDto getPrices(FigiesDto figiesDto);
}
