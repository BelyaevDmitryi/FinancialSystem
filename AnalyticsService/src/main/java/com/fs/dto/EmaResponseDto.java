package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmaResponseDto {
    private String figi;
    private BigDecimal ema;
    private LocalDateTime timestamp;
    private Integer period;
    /**
     * Серия значений EMA по периодам для построения графика (последнее значение = ema).
     */
    private List<BigDecimal> values;
}
