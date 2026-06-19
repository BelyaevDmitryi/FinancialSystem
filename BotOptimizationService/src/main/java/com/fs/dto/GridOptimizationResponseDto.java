package com.fs.dto;

import java.util.ArrayList;
import java.util.List;

public class GridOptimizationResponseDto {

    private String figi;
    private int totalRuns;
    private int passedFilters;
    private List<GridOptimizationRunResultDto> results = new ArrayList<>();

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }

    public int getPassedFilters() {
        return passedFilters;
    }

    public void setPassedFilters(int passedFilters) {
        this.passedFilters = passedFilters;
    }

    public List<GridOptimizationRunResultDto> getResults() {
        return results;
    }

    public void setResults(List<GridOptimizationRunResultDto> results) {
        this.results = results;
    }
}
