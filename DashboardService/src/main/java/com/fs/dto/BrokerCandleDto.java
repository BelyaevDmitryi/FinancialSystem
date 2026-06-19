package com.fs.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class BrokerCandleDto {
    private Instant time;
    private BigDecimal close;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }
}
