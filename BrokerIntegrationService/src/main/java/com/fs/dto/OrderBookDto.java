package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO для стакана заявок (order book)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookDto {
    /**
     * FIGI инструмента
     */
    private String figi;
    
    /**
     * Заявки на покупку (bids) - отсортированы по убыванию цены
     */
    private List<OrderBookEntryDto> bids;
    
    /**
     * Заявки на продажу (asks) - отсортированы по возрастанию цены
     */
    private List<OrderBookEntryDto> asks;
    
    /**
     * Последняя цена сделки
     */
    private BigDecimal lastPrice;
    
    /**
     * Время последнего обновления
     */
    private Instant timestamp;
    
    /**
     * Глубина стакана (количество уровней)
     */
    private Integer depth;
}
