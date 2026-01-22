package com.fs.service;

import com.fs.dto.StocksDto;
import com.fs.dto.TickersDto;
import com.fs.exception.StockNotFoundException;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockApiService {
    private final BrokerIntegrationServiceClient brokerIntegrationServiceClient;

    public StocksDto getStocksByTickers(TickersDto tickers) {
        log.info("Getting {} from BrokerIntegrationService", tickers.getTickers());
        try {
            StocksDto stocks = brokerIntegrationServiceClient.getStocksByTickers(tickers);
            if (stocks == null) {
                throw new StockNotFoundException("Stocks not found. Try another tickers.");
            }
            return stocks;
        } catch (Exception e) {
            log.error("Error getting stocks from BrokerIntegrationService: {}", e.getMessage());
            throw new StockNotFoundException("Stocks not found. Try another tickers.");
        }
    }
}
