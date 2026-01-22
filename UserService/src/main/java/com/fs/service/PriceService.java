package com.fs.service;

import com.fs.domain.Stock;
import com.fs.dto.PriceDataDto;
import com.fs.exception.PriceServiceException;
import com.fs.feignclient.PriceServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {
    private final PriceServiceClient priceServiceClient;

    public Map<String, BigDecimal> getPricesByFigies(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return new HashMap<>();
        }

        List<String> figies = stocks.stream()
                .map(Stock::getFigi)
                .filter(figi -> figi != null && !figi.isEmpty())
                .collect(Collectors.toList());

        if (figies.isEmpty()) {
            return new HashMap<>();
        }

        try {
            log.info("Getting prices for {} figies from PriceService", figies.size());
            List<PriceDataDto> priceDataList = priceServiceClient.getPricesByFigies(figies);
            
            Map<String, BigDecimal> prices = new HashMap<>();
            if (priceDataList != null) {
                priceDataList.forEach(priceData -> 
                    prices.put(priceData.getFigi(), priceData.getPrice())
                );
            }
            return prices;
        } catch (Exception e) {
            log.error("Error getting prices from PriceService: {}", e.getMessage());
            throw new PriceServiceException("Failed to get prices: " + e.getMessage());
        }
    }
}
