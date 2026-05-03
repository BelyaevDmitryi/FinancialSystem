package com.fs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public class SmaGridOptimizationRequest {

    @NotBlank
    private String figi;

    @NotNull
    private Instant from;

    @NotNull
    private Instant to;

    private String interval = "DAY";

    @Positive
    private int periodMin;

    @Positive
    private int periodMax;

    @Positive
    private int periodStep = 1;

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

    public int getPeriodMin() {
        return periodMin;
    }

    public void setPeriodMin(int periodMin) {
        this.periodMin = periodMin;
    }

    public int getPeriodMax() {
        return periodMax;
    }

    public void setPeriodMax(int periodMax) {
        this.periodMax = periodMax;
    }

    public int getPeriodStep() {
        return periodStep;
    }

    public void setPeriodStep(int periodStep) {
        this.periodStep = periodStep;
    }
}
