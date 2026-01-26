package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для одной записи в стакане (заявка на покупку или продажу)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookEntryDto {
    /**
     * Цена заявки
     */
    private BigDecimal price;
    
    /**
     * Количество (объем) заявки
     */
    private Long quantity;
}
