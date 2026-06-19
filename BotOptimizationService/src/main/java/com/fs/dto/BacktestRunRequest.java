package com.fs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public class BacktestRunRequest {

    @NotBlank
    private String figi;

    @NotNull
    private Instant from;

    @NotNull
    private Instant to;

    private String interval = "DAY";

    @Positive
    private int smaPeriod = 20;

    @NotNull
    @Positive
    private BigDecimal initialCash = BigDecimal.valueOf(100_000);

    private int slippageBps = 0;

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public Instant getFrom() {
        return from;
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(Instant to) {
        this.to = to;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public int getSmaPeriod() {
        return smaPeriod;
    }

    public void setSmaPeriod(int smaPeriod) {
        this.smaPeriod = smaPeriod;
    }

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash;
    }

    public int getSlippageBps() {
        return slippageBps;
    }

    public void setSlippageBps(int slippageBps) {
        this.slippageBps = slippageBps;
    }
}
