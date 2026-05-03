package com.fs.dto;

import java.util.List;

public class HistoricCandlesDto {
    private String figi;
    private String interval;
    private List<BrokerCandleDto> candles;

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public List<BrokerCandleDto> getCandles() {
        return candles;
    }

    public void setCandles(List<BrokerCandleDto> candles) {
        this.candles = candles;
    }
}
