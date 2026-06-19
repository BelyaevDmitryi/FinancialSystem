package com.fs.factory;

import com.fs.adapter.BrokerAdapter;
import com.fs.adapter.moex.MoexFigi;
import com.fs.dto.OrderBookDto;
import com.fs.exception.StockNotFoundException;
import com.fs.model.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        ensureAdapterMap();

        BrokerAdapter adapter = adapterMap.get(brokerName.toUpperCase());
        if (adapter == null) {
            log.warn("Broker adapter for '{}' not found, using default (TINKOFF)", brokerName);
            adapter = adapterMap.get("TINKOFF");
        }
        
        if (adapter == null || !adapter.isAvailable()) {
            log.warn("Broker adapter '{}' unavailable, trying first available", brokerName);
            adapter = brokerAdapters.stream()
                    .filter(BrokerAdapter::isAvailable)
                    .findFirst()
                    .orElse(null);
        }

        if (adapter == null) {
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

    /**
     * Tinkoff first, MOEX ISS on {@link StockNotFoundException}.
     */
    public Stock getStockByTickerWithFallback(String ticker) {
        ensureAdapterMap();

        BrokerAdapter tinkoff = adapterMap.get("TINKOFF");
        if (tinkoff != null && tinkoff.isAvailable()) {
            try {
                return tinkoff.getStockByTicker(ticker);
            } catch (StockNotFoundException ex) {
                log.info("Stock {} not found in TINKOFF, trying MOEX ISS", ticker);
            }
        }

        BrokerAdapter moex = adapterMap.get("MOEX_ISS");
        if (moex != null && moex.isAvailable()) {
            try {
                return moex.getStockByTicker(ticker);
            } catch (StockNotFoundException ex) {
                log.info("Stock {} not found in MOEX ISS", ticker);
            }
        }

        throw new StockNotFoundException(
                String.format("Stock %s not found in TINKOFF or MOEX ISS", ticker));
    }

    /**
     * Tinkoff order book by real FIGI; for {@code MOEX:{TICKER}} — resolve ticker in Tinkoff or MOEX ISS.
     */
    public OrderBookDto getOrderBookWithFallback(String figi, int depth) {
        ensureAdapterMap();

        Optional<String> moexTicker = MoexFigi.toTicker(figi);
        if (moexTicker.isPresent()) {
            return getOrderBookForMoexFigi(figi, moexTicker.get(), depth);
        }

        BrokerAdapter tinkoff = adapterMap.get("TINKOFF");
        if (tinkoff != null && tinkoff.isAvailable()) {
            return tinkoff.getOrderBook(figi, depth);
        }

        throw new IllegalStateException("No available broker adapter for order book");
    }

    private OrderBookDto getOrderBookForMoexFigi(String figi, String ticker, int depth) {
        BrokerAdapter tinkoff = adapterMap.get("TINKOFF");
        if (tinkoff != null && tinkoff.isAvailable()) {
            try {
                Stock stock = tinkoff.getStockByTicker(ticker);
                if (MoexFigi.toTicker(stock.getFigi()).isEmpty()) {
                    return tinkoff.getOrderBook(stock.getFigi(), depth);
                }
            } catch (StockNotFoundException ex) {
                log.info("Order book: stock {} not found in TINKOFF, trying MOEX ISS", ticker);
            }
        }

        BrokerAdapter moex = adapterMap.get("MOEX_ISS");
        if (moex != null && moex.isAvailable()) {
            return moex.getOrderBook(figi, depth);
        }

        throw new StockNotFoundException(
                String.format("Order book for %s not found in TINKOFF or MOEX ISS", figi));
    }

    private void ensureAdapterMap() {
        if (adapterMap == null) {
            adapterMap = brokerAdapters.stream()
                    .collect(Collectors.toMap(
                            BrokerAdapter::getBrokerName,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
        }
    }
}
