package com.fs.adapter;

import com.fs.dto.FigiesDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.OrderBookDto;
import com.fs.dto.BrokerCandleDto;
import com.fs.model.Stock;

import java.time.Instant;
import java.util.List;

/**
 * Интерфейс для адаптеров различных брокеров.
 * Позволяет легко добавлять поддержку новых брокеров (ВТБ, Сбер, MOEX и т.д.)
 */
public interface BrokerAdapter {
    
    /**
     * Получить информацию об акции по тикеру
     */
    Stock getStockByTicker(String ticker);
    
    /**
     * Получить информацию об акциях по списку тикеров
     */
    StocksDto getStocksByTickers(TickersDto tickersDto);
    
    /**
     * Получить цены по списку FIGI
     */
    StocksPricesDto getPrices(FigiesDto figiesDto);
    
    /**
     * Получить стакан заявок (order book) по FIGI
     * @param figi FIGI инструмента
     * @param depth глубина стакана (количество уровней)
     * @return стакан заявок
     */
    OrderBookDto getOrderBook(String figi, int depth);
    
    /**
     * Получить список всех доступных тикеров (акций) с биржи
     */
    StocksDto getAvailableTickers();
    
    /**
     * Выставить заявку на биржу
     * @param accountId ID счета пользователя у брокера
     * @param createOrderDto данные заявки
     * @return информация о созданной заявке
     */
    BrokerOrderDto placeOrder(String accountId, CreateBrokerOrderDto createOrderDto);
    
    /**
     * Отменить заявку на бирже
     * @param accountId ID счета пользователя у брокера
     * @param orderId ID заявки на бирже
     */
    void cancelOrder(String accountId, String orderId);

    /**
     * Изменить параметры активной заявки (цена, количество).
     */
    BrokerOrderDto amendOrder(String accountId, String orderId, AmendBrokerOrderDto amendDto);
    
    /**
     * Получить статус заявки
     * @param accountId ID счета пользователя у брокера
     * @param orderId ID заявки на бирже
     * @return информация о заявке
     */
    BrokerOrderDto getOrderStatus(String accountId, String orderId);
    
    /**
     * Получить все активные заявки пользователя
     * @param accountId ID счета пользователя у брокера
     * @return список активных заявок
     */
    List<BrokerOrderDto> getActiveOrders(String accountId);
    
    /**
     * Получить название брокера
     */
    String getBrokerName();
    
    /**
     * Проверить доступность брокера
     */
    boolean isAvailable();

    /**
     * Исторические свечи по инструменту за период.
     *
     * @param interval код интервала (например DAY, HOUR, MIN_5, MIN_15, MIN_1)
     */
    List<BrokerCandleDto> getHistoricCandles(String figi, Instant from, Instant to, String interval);
}
