package com.fs.dto;

import com.fs.domain.BrokerOrderType;
import com.fs.domain.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CreateOrderDto {
    @NotBlank(message = "FIGI не может быть пустым")
    private String figi;
    
    @NotNull(message = "Тип ордера обязателен")
    private OrderType type;
    
    @NotNull(message = "Количество обязательно")
    @Positive(message = "Количество должно быть положительным")
    private BigDecimal quantity;
    
    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительной")
    private BigDecimal price;
    
    private String comment;

    /**
     * Тип исполнения у брокера: MARKET, LIMIT, STOP (по умолчанию LIMIT).
     */
    private BrokerOrderType orderType;

    /**
     * Цена активации стоп-заявки (для STOP).
     */
    private BigDecimal stopPrice;

    /**
     * Paper-режим: симуляция fill без вызова брокера.
     */
    private Boolean paper;

    public CreateOrderDto(String figi, OrderType type, BigDecimal quantity, BigDecimal price, String comment) {
        this.figi = figi;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.comment = comment;
    }
}
