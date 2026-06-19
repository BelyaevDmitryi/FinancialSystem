package com.fs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fs.domain.FigiWithPrice;
import com.fs.dto.Stock;
import com.fs.dto.StockWithPrice;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksWithPrices;
import com.fs.exception.StockAlreadyExistException;
import com.fs.exception.StockNotFoundException;
import com.fs.feignclient.StockServiceClient;
import com.fs.repository.StockRepository;

import com.fs.dto.PriceDataDto;
import com.fs.dto.FigiesDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.StockPrice;
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
        List<String> figies = notFoundInRepo.stream()
                .map(Stock::getFigi)
                .toList();

        FigiesDto figiesDto = new FigiesDto(figies);
        StocksPricesDto stocksPricesDto = stockServiceClient.getPrices(figiesDto);

        Map<String, BigDecimal> priceMap = stocksPricesDto.getPrices().stream()
                .collect(Collectors.toMap(
                        StockPrice::getFigi,
                        StockPrice::getPrice
                ));

        Map<String, String> priceSourceByFigi = new java.util.HashMap<>();
        stocksPricesDto.getPrices().forEach(stockPrice ->
                priceSourceByFigi.put(stockPrice.getFigi(), "TINKOFF"));

        List<Stock> missingAfterTinkoff = notFoundInRepo.stream()
                .filter(stock -> !priceMap.containsKey(stock.getFigi()))
                .toList();

        if (!missingAfterTinkoff.isEmpty()) {
            applyMoexPriceFallback(missingAfterTinkoff, priceMap, priceSourceByFigi);
        }

        List<StockWithPrice> result = notFoundInRepo.stream()
                .filter(stock -> priceMap.containsKey(stock.getFigi()))
                .map(stock -> new StockWithPrice(
                        stock,
                        priceMap.get(stock.getFigi()),
                        priceSourceByFigi.getOrDefault(stock.getFigi(), stock.getSource())))
                .toList();

        log.info("Time for getting from API - {}", System.currentTimeMillis() - start);
        return result;
    }

    private void applyMoexPriceFallback(List<Stock> missingStocks,
            Map<String, BigDecimal> priceMap,
            Map<String, String> priceSourceByFigi) {
        List<String> moexFigies = missingStocks.stream()
                .map(stock -> "MOEX:" + stock.getTicker().toUpperCase())
                .toList();
        Map<String, String> moexFigiToOriginal = missingStocks.stream()
                .collect(Collectors.toMap(
                        stock -> "MOEX:" + stock.getTicker().toUpperCase(),
                        Stock::getFigi,
                        (left, right) -> left));

        StocksPricesDto moexPrices = stockServiceClient.getPrices(new FigiesDto(moexFigies), "MOEX_ISS");
        moexPrices.getPrices().forEach(stockPrice -> {
            String originalFigi = moexFigiToOriginal.get(stockPrice.getFigi());
            if (originalFigi != null) {
                priceMap.put(originalFigi, stockPrice.getPrice());
                priceSourceByFigi.put(originalFigi, "MOEX_ISS");
            }
        });
    }
    
    /**
     * Получить цены по списку FIGI (новый упрощенный метод)
     */
    public List<PriceDataDto> getPricesByFigies(List<String> figies) {
        long start = System.currentTimeMillis();
        List<PriceDataDto> result = new ArrayList<>();
        
        // Проверяем кеш в Redis
        List<FigiWithPrice> fromRedis = stockRepository.findAllById(figies);
        List<String> foundFigies = fromRedis.stream()
                .map(FigiWithPrice::getFigi)
                .toList();
        
        fromRedis.forEach(fwp -> result.add(new PriceDataDto(
                fwp.getFigi(),
                fwp.getPrice(),
                java.time.LocalDateTime.now()
        )));
        
        // Запрашиваем недостающие из API
        List<String> notFoundFigies = figies.stream()
                .filter(figi -> !foundFigies.contains(figi))
                .toList();
        
        if (!notFoundFigies.isEmpty()) {
            FigiesDto figiesDto = new FigiesDto(notFoundFigies);
            StocksPricesDto stocksPricesDto = stockServiceClient.getPrices(figiesDto);
            
            // Сохраняем в кеш
            List<FigiWithPrice> toSave = stocksPricesDto.getPrices().stream()
                    .map(sp -> new FigiWithPrice(sp.getFigi(), sp.getPrice(), "TINKOFF"))
                    .toList();
            stockRepository.saveAll(toSave);
            
            // Добавляем в результат
            stocksPricesDto.getPrices().forEach(sp -> result.add(new PriceDataDto(
                    sp.getFigi(),
                    sp.getPrice(),
                    java.time.LocalDateTime.now()
            )));
        }
        
        log.info("Time for getting prices by figies - {} ms", System.currentTimeMillis() - start);
        return result;
    }

    public List<PriceDataDto> getSnapshotPricesByFigies(List<String> figies) {
        if (figies == null || figies.isEmpty()) {
            return List.of();
        }

        return stockRepository.findAllById(figies).stream()
                .map(fwp -> new PriceDataDto(
                        fwp.getFigi(),
                        fwp.getPrice(),
                        java.time.LocalDateTime.now()))
                .toList();
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
