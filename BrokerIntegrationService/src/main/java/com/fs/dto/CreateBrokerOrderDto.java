package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для создания заявки на бирже через брокера
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBrokerOrderDto {
    /**
     * FIGI инструмента
     */
    private String figi;
    
    /**
     * Количество лотов
     */
    private Long quantity;
    
    /**
     * Цена за единицу (null для рыночной заявки)
     */
    private BigDecimal price;
    
    /**
     * Тип заявки: BUY или SELL
     */
    private String direction;
    
    /**
     * Тип заявки: LIMIT (лимитная) или MARKET (рыночная)
     */
    private String orderType;
    
    /**
     * Комментарий к заявке
     */
    private String comment;
}
