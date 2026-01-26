package com.fs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsRequestDto {
    @NotBlank(message = "FIGI не может быть пустым")
    private String figi;
    
    private List<PriceDataDto> priceData;
    
    @NotNull(message = "Период не может быть null")
    @Positive(message = "Период должен быть положительным числом")
    private Integer period;
}
