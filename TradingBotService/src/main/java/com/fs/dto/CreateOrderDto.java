package com.fs.dto;

import com.fs.domain.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDto {
    private String figi;
    private OrderType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private String comment;

    /** Тип исполнения у брокера: MARKET, LIMIT, STOP (P0 default — MARKET). */
    private String orderType;

    /** Paper-режим: симуляция fill в Terminal без брокера. */
    private Boolean paper;
}
