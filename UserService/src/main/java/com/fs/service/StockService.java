package com.fs.service;

import com.fs.domain.Stock;
import com.fs.domain.Type;
import com.fs.exception.StockNotFoundException;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fs.repository.StockRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final BrokerIntegrationServiceClient brokerIntegrationServiceClient;

    @Transactional
    public Stock getStockByTicker(String ticker) {
        // Сначала пытаемся найти в локальной БД
        return stockRepository.findByTicker(ticker)
                .orElseGet(() -> {
                    // Если не найдено, запрашиваем у BrokerIntegrationService
                    log.info("Stock {} not found in local DB, fetching from BrokerIntegrationService", ticker);
                    try {
                        com.fs.model.Stock brokerStock = brokerIntegrationServiceClient.getStock(ticker);
                        // Преобразуем и сохраняем в БД
                        Stock domainStock = convertToDomainStock(brokerStock);
                        return stockRepository.save(domainStock);
                    } catch (Exception e) {
                        log.error("Error fetching stock {} from BrokerIntegrationService: {}", ticker, e.getMessage());
                        throw new StockNotFoundException("Stock not found. Try another ticker.");
                    }
                });
    }
    
    private Stock convertToDomainStock(com.fs.model.Stock brokerStock) {
        // Преобразуем Currency из com.fs.model.Currency в com.fs.domain.Currency
        com.fs.domain.Currency domainCurrency = com.fs.domain.Currency.valueOf(brokerStock.getCurrency().name());
        
        // Преобразуем Type из String в enum Type
        Type domainType;
        try {
            domainType = Type.fromValue(brokerStock.getType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown stock type: {}, defaulting to SHARE_TYPE_COMMON", brokerStock.getType());
            domainType = Type.SHARE_TYPE_COMMON;
        }
        
        return new Stock(
                brokerStock.getTicker(),
                brokerStock.getFigi(),
                domainCurrency,
                brokerStock.getName(),
                domainType,
                null, // price будет обновлен позже через PriceService
                brokerStock.getSource()
        );
    }

    public void deleteStockByTicker(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new StockNotFoundException("Stock not found. Try another ticker."));
        stockRepository.delete(stock);
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public List<Stock> getStocksByTickers(List<String> tickers) {
        return stockRepository.findByTickerIn(tickers);
    }
}
