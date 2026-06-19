package com.fs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GridOptimizationRequest {

    @NotBlank
    private String figi;

    @NotNull
    private Instant from;

    @NotNull
    private Instant to;

    private String interval = "DAY";

    @NotNull
    @Positive
    private BigDecimal initialCash = BigDecimal.valueOf(100_000);

    private int slippageBps;

    @NotEmpty
    @Valid
    private List<GridParameterSpecDto> parameters = new ArrayList<>();

    @Valid
    private GridOptimizationFiltersDto filters;

    @Positive
    private int parallelPoolSize = 4;

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

    public List<GridParameterSpecDto> getParameters() {
        return parameters;
    }

    public void setParameters(List<GridParameterSpecDto> parameters) {
        this.parameters = parameters;
    }

    public GridOptimizationFiltersDto getFilters() {
        return filters;
    }

    public void setFilters(GridOptimizationFiltersDto filters) {
        this.filters = filters;
    }

    public int getParallelPoolSize() {
        return parallelPoolSize;
    }

    public void setParallelPoolSize(int parallelPoolSize) {
        this.parallelPoolSize = parallelPoolSize;
    }
}
