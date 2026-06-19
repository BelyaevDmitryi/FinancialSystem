package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для изменения параметров заявки у брокера.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmendBrokerOrderDto {

    private BigDecimal price;

    private Long quantity;
}
