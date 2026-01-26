package com.fs.service;

import com.fs.domain.FigiWithPrice;
import com.fs.dto.FigiesDto;
import com.fs.dto.StockPrice;
import com.fs.dto.StocksPricesDto;
import com.fs.feignclient.StockServiceClient;
import com.fs.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для периодического обновления цен на акции
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceMonitoringService {
    
    private final StockRepository stockRepository;
    private final StockServiceClient stockServiceClient;
    
    /**
     * Периодическое обновление цен для всех отслеживаемых инструментов
     * Выполняется каждые 30 секунд
     */
    @Scheduled(fixedDelayString = "${price.monitoring.update-interval:30000}")
    public void updatePrices() {
        try {
            log.debug("Начало периодического обновления цен");
            
            // Получаем все FIGI из кеша
            List<FigiWithPrice> allStocks = (List<FigiWithPrice>) stockRepository.findAll();
            
            if (allStocks.isEmpty()) {
                log.debug("Нет инструментов для обновления цен");
                return;
            }
            
            List<String> figies = allStocks.stream()
                    .map(FigiWithPrice::getFigi)
                    .toList();
            
            log.info("Обновление цен для {} инструментов", figies.size());
            
            // Запрашиваем актуальные цены через BrokerIntegrationService
            FigiesDto figiesDto = new FigiesDto(figies);
            StocksPricesDto stocksPricesDto = stockServiceClient.getPrices(figiesDto);
            
            // Обновляем цены в кеше
            List<FigiWithPrice> updatedStocks = stocksPricesDto.getPrices().stream()
                    .map(sp -> {
                        FigiWithPrice existing = stockRepository.findById(sp.getFigi())
                                .orElse(new FigiWithPrice(sp.getFigi(), sp.getPrice(), "TINKOFF"));
                        existing.setPrice(sp.getPrice());
                        return existing;
                    })
                    .toList();
            
            stockRepository.saveAll(updatedStocks);
            
            log.info("Цены обновлены для {} инструментов", updatedStocks.size());
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении цен: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Обновить цены для конкретных FIGI
     */
    public void updatePricesForFigies(List<String> figies) {
        if (figies == null || figies.isEmpty()) {
            return;
        }
        
        try {
            log.debug("Обновление цен для {} инструментов", figies.size());
            
            FigiesDto figiesDto = new FigiesDto(figies);
            StocksPricesDto stocksPricesDto = stockServiceClient.getPrices(figiesDto);
            
            List<FigiWithPrice> updatedStocks = stocksPricesDto.getPrices().stream()
                    .map(sp -> {
                        FigiWithPrice existing = stockRepository.findById(sp.getFigi())
                                .orElse(new FigiWithPrice(sp.getFigi(), sp.getPrice(), "TINKOFF"));
                        existing.setPrice(sp.getPrice());
                        return existing;
                    })
                    .toList();
            
            stockRepository.saveAll(updatedStocks);
            
            log.info("Цены обновлены для {} инструментов", updatedStocks.size());
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении цен для FIGI {}: {}", figies, e.getMessage(), e);
        }
    }
}
