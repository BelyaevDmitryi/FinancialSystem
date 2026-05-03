package com.fs.service;

import com.fs.adapter.BrokerAdapter;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.FigiesDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.OrderBookDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.factory.BrokerFactory;
import com.fs.model.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Сервис интеграции с брокерами.
 * Использует фабрику для выбора подходящего брокера.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerIntegrationService {
    
    private final BrokerFactory brokerFactory;
    
    @Value("${broker.default:TINKOFF}")
    private String defaultBroker;
    
    /**
     * Получить информацию об акции по тикеру
     */
    public Stock getStockByTicker(String ticker) {
        return getStockByTicker(ticker, defaultBroker);
    }
    
    /**
     * Получить информацию об акции по тикеру с указанием брокера
     */
    public Stock getStockByTicker(String ticker, String brokerName) {
        log.debug("Getting stock {} from broker {}", ticker, brokerName);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getStockByTicker(ticker);
    }
    
    /**
     * Получить информацию об акциях по списку тикеров
     */
    public StocksDto getStocksByTickers(TickersDto tickersDto) {
        return getStocksByTickers(tickersDto, defaultBroker);
    }
    
    /**
     * Получить информацию об акциях по списку тикеров с указанием брокера
     */
    public StocksDto getStocksByTickers(TickersDto tickersDto, String brokerName) {
        log.debug("Getting stocks {} from broker {}", tickersDto.getTickers(), brokerName);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getStocksByTickers(tickersDto);
    }
    
    /**
     * Получить цены по списку FIGI
     */
    public StocksPricesDto getPrices(FigiesDto figiesDto) {
        return getPrices(figiesDto, defaultBroker);
    }
    
    /**
     * Получить цены по списку FIGI с указанием брокера
     */
    public StocksPricesDto getPrices(FigiesDto figiesDto, String brokerName) {
        log.debug("Getting prices for {} figies from broker {}", figiesDto.getFigies().size(), brokerName);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getPrices(figiesDto);
    }
    
    /**
     * Получить стакан заявок по FIGI
     */
    public OrderBookDto getOrderBook(String figi, int depth) {
        return getOrderBook(figi, depth, defaultBroker);
    }
    
    /**
     * Получить стакан заявок по FIGI с указанием брокера
     */
    public OrderBookDto getOrderBook(String figi, int depth, String brokerName) {
        log.debug("Getting order book for figi {} from broker {} with depth {}", figi, brokerName, depth);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getOrderBook(figi, depth);
    }
    
    /**
     * Получить список доступных тикеров с биржи
     */
    public StocksDto getAvailableTickers() {
        return getAvailableTickers(defaultBroker);
    }
    
    /**
     * Получить список доступных тикеров с биржи с указанием брокера
     */
    public StocksDto getAvailableTickers(String brokerName) {
        log.debug("Getting available tickers from broker {}", brokerName);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getAvailableTickers();
    }
    
    /**
     * Выставить заявку на биржу
     */
    public BrokerOrderDto placeOrder(String accountId, CreateBrokerOrderDto createOrderDto) {
        return placeOrder(accountId, createOrderDto, defaultBroker);
    }
    
    /**
     * Выставить заявку на биржу с указанием брокера
     */
    public BrokerOrderDto placeOrder(String accountId, CreateBrokerOrderDto createOrderDto, String brokerName) {
        log.debug("Placing order on broker {} for account {}", brokerName, accountId);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.placeOrder(accountId, createOrderDto);
    }
    
    /**
     * Отменить заявку на бирже
     */
    public void cancelOrder(String accountId, String orderId) {
        cancelOrder(accountId, orderId, defaultBroker);
    }
    
    /**
     * Отменить заявку на бирже с указанием брокера
     */
    public void cancelOrder(String accountId, String orderId, String brokerName) {
        log.debug("Cancelling order {} on broker {} for account {}", orderId, brokerName, accountId);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        adapter.cancelOrder(accountId, orderId);
    }
    
    /**
     * Получить статус заявки
     */
    public BrokerOrderDto getOrderStatus(String accountId, String orderId) {
        return getOrderStatus(accountId, orderId, defaultBroker);
    }
    
    /**
     * Получить статус заявки с указанием брокера
     */
    public BrokerOrderDto getOrderStatus(String accountId, String orderId, String brokerName) {
        log.debug("Getting order status {} from broker {} for account {}", orderId, brokerName, accountId);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getOrderStatus(accountId, orderId);
    }
    
    /**
     * Получить все активные заявки пользователя
     */
    public List<BrokerOrderDto> getActiveOrders(String accountId) {
        return getActiveOrders(accountId, defaultBroker);
    }
    
    /**
     * Получить все активные заявки пользователя с указанием брокера
     */
    public List<BrokerOrderDto> getActiveOrders(String accountId, String brokerName) {
        log.debug("Getting active orders from broker {} for account {}", brokerName, accountId);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        return adapter.getActiveOrders(accountId);
    }
    
    /**
     * Получить список доступных брокеров
     */
    public List<String> getAvailableBrokers() {
        return brokerFactory.getAvailableBrokers();
    }

    /**
     * Исторические свечи по FIGI за период.
     */
    public HistoricCandlesDto getHistoricCandles(String figi, Instant from, Instant to, String interval) {
        return getHistoricCandles(figi, from, to, interval, defaultBroker);
    }

    /**
     * Исторические свечи с указанием брокера.
     */
    public HistoricCandlesDto getHistoricCandles(String figi, Instant from, Instant to, String interval,
                                                   String brokerName) {
        log.debug("Getting historic candles for {} from broker {} interval {}", figi, brokerName, interval);
        BrokerAdapter adapter = brokerFactory.getBrokerAdapter(brokerName);
        List<BrokerCandleDto> candles = adapter.getHistoricCandles(figi, from, to, interval);
        return new HistoricCandlesDto(figi, interval, candles);
    }
}
