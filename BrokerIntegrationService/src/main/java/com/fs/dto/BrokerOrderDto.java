package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO для информации о заявке на бирже
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrokerOrderDto {
    /**
     * ID заявки на бирже
     */
    private String orderId;
    
    /**
     * FIGI инструмента
     */
    private String figi;
    
    /**
     * Количество лотов
     */
    private Long quantity;
    
    /**
     * Исполнено лотов
     */
    private Long executedQuantity;
    
    /**
     * Цена за единицу
     */
    private BigDecimal price;
    
    /**
     * Средняя цена исполнения
     */
    private BigDecimal averageExecutionPrice;
    
    /**
     * Тип заявки: BUY или SELL
     */
    private String direction;
    
    /**
     * Статус заявки (NEW, FILL, PARTIALLY_FILLED, CANCELLED, REJECTED)
     */
    private String status;
    
    /**
     * Тип заявки: LIMIT или MARKET
     */
    private String orderType;
    
    /**
     * Время создания заявки
     */
    private Instant createdAt;
    
    /**
     * Время исполнения заявки
     */
    private Instant executedAt;
    
    /**
     * Сообщение об ошибке (если есть)
     */
    private String message;
}
