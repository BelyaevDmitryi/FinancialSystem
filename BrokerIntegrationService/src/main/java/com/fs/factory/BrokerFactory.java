package com.fs.factory;

import com.fs.adapter.BrokerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Фабрика для получения адаптера брокера.
 * Поддерживает несколько брокеров с автоматическим fallback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrokerFactory {
    
    private final List<BrokerAdapter> brokerAdapters;
    private Map<String, BrokerAdapter> adapterMap;
    
    /**
     * Получить адаптер брокера по имени
     */
    public BrokerAdapter getBrokerAdapter(String brokerName) {
        if (adapterMap == null) {
            adapterMap = brokerAdapters.stream()
                    .collect(Collectors.toMap(
                            BrokerAdapter::getBrokerName,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
        }
        
        BrokerAdapter adapter = adapterMap.get(brokerName.toUpperCase());
        if (adapter == null) {
            log.warn("Broker adapter for '{}' not found, using default (TINKOFF)", brokerName);
            adapter = adapterMap.get("TINKOFF");
        }
        
        if (adapter == null || !adapter.isAvailable()) {
            throw new IllegalStateException("No available broker adapter found");
        }
        
        return adapter;
    }
    
    /**
     * Получить первый доступный адаптер
     */
    public BrokerAdapter getAvailableBrokerAdapter() {
        return brokerAdapters.stream()
                .filter(BrokerAdapter::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available broker adapter found"));
    }
    
    /**
     * Получить список всех доступных брокеров
     */
    public List<String> getAvailableBrokers() {
        return brokerAdapters.stream()
                .filter(BrokerAdapter::isAvailable)
                .map(BrokerAdapter::getBrokerName)
                .collect(Collectors.toList());
    }
}
