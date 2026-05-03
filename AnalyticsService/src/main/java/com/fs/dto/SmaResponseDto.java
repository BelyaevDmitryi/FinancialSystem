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
public class SmaResponseDto {
    private String figi;
    private BigDecimal sma;
    private LocalDateTime timestamp;
    private Integer period;
    /**
     * Серия значений SMA по периодам для построения графика (последнее значение = sma).
     */
    private List<BigDecimal> values;
}
