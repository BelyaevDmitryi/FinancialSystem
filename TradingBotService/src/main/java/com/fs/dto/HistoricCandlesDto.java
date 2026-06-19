package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricCandlesDto {
    private String figi;
    private String interval;
    private List<BrokerCandleDto> candles;
}
