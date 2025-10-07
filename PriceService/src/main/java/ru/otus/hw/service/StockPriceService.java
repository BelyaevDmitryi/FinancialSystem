package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.hw.domain.FigiWithPrice;
import ru.otus.hw.dto.Stock;
import ru.otus.hw.dto.StockWithPrice;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksWithPrices;
import ru.otus.hw.exception.StockAlreadyExistException;
import ru.otus.hw.exception.StockNotFoundException;
import ru.otus.hw.feignclient.StockServiceClient;
import ru.otus.hw.repository.StockRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceService {
    private final StockRepository stockRepository;
    private final StockServiceClient stockServiceClient;

    public FigiWithPrice addStock(FigiWithPrice figiWithPrice) {
        if(stockRepository.existsById(figiWithPrice.getFigi())) {
            throw new StockAlreadyExistException("Stock already exist.");
        }

        return stockRepository.save(figiWithPrice);
    }

    public StocksWithPrices getPrices(StocksDto stocksDto) {
        long start = System.currentTimeMillis();
        List<StockWithPrice> result = new ArrayList<>();
        List<Stock> searchStockPrices = new ArrayList<>(stocksDto.getStocks());
        List<StockWithPrice> fromReddis = getFromReddis(searchStockPrices);
        List<String> foundedFigiesFromReddis = fromReddis.stream().map(StockWithPrice::getFigi).toList();
        result.addAll(fromReddis);

        List<Stock> notFoundInRepo = searchStockPrices.stream()
                .filter(s -> !foundedFigiesFromReddis.contains(s.getFigi())).toList();

        if(!notFoundInRepo.isEmpty()) {
            List<StockWithPrice> fromApi = getPricesFromApi(notFoundInRepo);
            result.addAll(fromApi);
            saveToRedis(fromApi);
        }

        checkAllOk(searchStockPrices, result);
        log.info("All time - {}", System.currentTimeMillis() - start);
        return new StocksWithPrices(result);
    }

    private List<StockWithPrice> getFromReddis(List<Stock> searchStockPrices) {
        long start = System.currentTimeMillis();
        List<StockWithPrice> stocksFromReddis = new ArrayList<>();
        List<String> figies = searchStockPrices.stream().map(Stock::getFigi).toList();

        log.info("Getting from Redis {}", figies);
        List<FigiWithPrice> foundedFigies = new ArrayList<>(stockRepository.findAllById(figies));
        log.info("Founded figies in Redis {}", foundedFigies);

        if(!foundedFigies.isEmpty()) {
            Map<String, BigDecimal> figiWithPrice = foundedFigies.stream()
                    .collect(Collectors.toMap(FigiWithPrice::getFigi, FigiWithPrice::getPrice));

            searchStockPrices.stream()
                    .filter(s -> figiWithPrice.containsKey(s.getFigi()))
                    .forEach(s -> stocksFromReddis.add(new StockWithPrice(s, figiWithPrice.get(s.getFigi()))));
        }
        log.info("Time for getting from Reddis - {}", System.currentTimeMillis() - start);
        return stocksFromReddis;
    }

    private void saveToRedis(List<StockWithPrice> stocks) {
        List<FigiWithPrice> toSave = stocks.stream()
                .map(s -> new FigiWithPrice(s.getFigi(), s.getPrice(), s.getSource()))
                .toList();

        log.info("Saving to Redis {}", toSave);
        stockRepository.saveAll(toSave);
        log.info("Saved to Redis successfully");
    }

    private List<StockWithPrice> getPricesFromApi(List<Stock> notFoundInRepo) {
        long start = System.currentTimeMillis();
        StocksDto stocksDto = new StocksDto(notFoundInRepo);
        StocksWithPrices stocksWithPrices = stockServiceClient.getPrices(stocksDto);
        log.info("Time for getting from API - {}", System.currentTimeMillis() - start);
        return stocksWithPrices.getStocks();
    }

    private void checkAllOk(List<Stock> inputStocks, List<StockWithPrice> result) {
        if(inputStocks.size() != result.size()) {
            List<String> foundedStocks = result.stream().map(StockWithPrice::getTicker).toList();
            List<Stock> stockNotFound = inputStocks.stream()
                    .filter(s -> !foundedStocks.contains(s.getTicker()))
                    .toList();

            throw new StockNotFoundException(String.format("Stocks %s not found", stockNotFound));
        }
    }
}
