package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.hw.dto.FigiesDto;
import ru.otus.hw.dto.StockPrice;
import ru.otus.hw.dto.StockWithPrice;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksPricesDto;
import ru.otus.hw.dto.StocksWithPrices;
import ru.otus.hw.dto.TickersDto;
import ru.otus.hw.exception.StockNotFoundException;
import ru.otus.hw.feignclient.ApiStockService;
import ru.otus.hw.feignclient.TinkoffServiceClient;
import ru.otus.hw.model.Stock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private final TinkoffServiceClient tinkoffService;

    public StocksDto getStocksByTickers(TickersDto tickersDto) {
        log.info("Try to get stocks with tickers - {}", tickersDto.getTickers());
        List<Stock> resultList = new ArrayList<>();
        List<String> tickers = new ArrayList<>(tickersDto.getTickers());
        List<Stock> stocksFromTinkoff = tinkoffService.getStocksByTickers(tickersDto).getStocks();
        resultList.addAll(stocksFromTinkoff);
        List<String> tickersFromTinkoff = stocksFromTinkoff.stream().map(s -> s.getTicker()).collect(Collectors.toList());
        log.info("Successful received from Tinkoff - {}", tickersFromTinkoff);
        tickers.removeAll(tickersFromTinkoff);

        if(!tickers.isEmpty()) {
            throw new StockNotFoundException(String.format("Tickers % not found.", tickers));
        }

        return new StocksDto(resultList);
    }

    public StocksWithPrices getPrices(StocksDto stocksDto) {
        List<StockWithPrice> result = new ArrayList<>();
        List<Stock> fromMoex = new ArrayList<>();
        List<Stock> fromTinkoff = new ArrayList<>();
        sortBySource(stocksDto.getStocks(), fromMoex, fromTinkoff);

//        getPricesByService(moexService, fromMoex, result);
        getPricesByService(tinkoffService, fromTinkoff, result);

        checkAllOk(stocksDto.getStocks(), result);
        return new StocksWithPrices(result);
    }

    private void sortBySource(List<Stock> stocks, List<Stock> fromMoex, List<Stock> fromTinkoff) {
        stocks.forEach(s -> {
            if(s.getSource().equals("MOEX")) {
                fromMoex.add(s);
            } else if (s.getSource().equals("TINKOFF")) {
                fromTinkoff.add(s);
            } else {
                throw new StockNotFoundException(String.format("Source - %s not found", s.getSource()));
            }
        });
    }

    private void getPricesByService(ApiStockService stockService, List<Stock> stocks, List<StockWithPrice> result) {
        if(!stocks.isEmpty()) {
            StocksPricesDto prices = stockService.getPrices(
                    new FigiesDto(stocks.stream().map(m -> m.getFigi()).collect(Collectors.toList())));
            Map<String, BigDecimal> pricesFigi = prices.getPrices().stream()
                    .collect(Collectors.toMap(StockPrice::getFigi, StockPrice::getPrice));
            stocks.forEach(fm -> result.add(new StockWithPrice(fm, pricesFigi.get(fm.getFigi()))));
        }
    }

    private void checkAllOk(List<Stock> inputStocks, List<StockWithPrice> result) {
        if(inputStocks.size() != result.size()) {
            List<String> foundedStocks = result.stream().map(sp -> sp.getTicker()).collect(Collectors.toList());
            List<Stock> stockNotFound = inputStocks.stream()
                    .filter(s -> !foundedStocks.contains(s.getTicker()))
                    .collect(Collectors.toList());

            throw new StockNotFoundException(String.format("Stocks %s not found", stockNotFound));
        }
    }
}
