package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для создания заявки на бирже через брокера.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBrokerOrderDto {
    /**
     * FIGI инструмента.
     */
    private String figi;

    /**
     * Количество лотов.
     */
    private Long quantity;

    /**
     * Цена за единицу (null для рыночной заявки).
     */
    private BigDecimal price;

    /**
     * Направление: BUY или SELL.
     */
    private String direction;

    /**
     * Тип заявки: MARKET, LIMIT или STOP.
     */
    private String orderType;

    /**
     * Цена активации стоп-заявки (обязательна для STOP).
     */
    private BigDecimal stopPrice;

    /**
     * Комментарий к заявке.
     */
    private String comment;
}
