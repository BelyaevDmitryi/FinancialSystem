package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsRequestDto {
    private String figi;
    private List<PriceDataDto> priceData;
    private Integer period;
}
