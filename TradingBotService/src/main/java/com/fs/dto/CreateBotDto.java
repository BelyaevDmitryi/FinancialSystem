package com.fs.dto;

import com.fs.domain.BotStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBotDto {
    @NotBlank(message = "FIGI не может быть пустым")
    private String figi;
    
    @NotBlank(message = "Название бота обязательно")
    private String name;
    
    @NotNull(message = "Стратегия обязательна")
    private BotStrategy strategy;
    
    @NotNull(message = "Максимальный размер позиции обязателен")
    @Positive(message = "Максимальный размер позиции должен быть положительным")
    private BigDecimal maxPositionSize;
    
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer smaPeriod;
    private Integer emaPeriod;
}
