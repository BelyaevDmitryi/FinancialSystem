package com.fs.dto;

public class OptimizationResultDto {
    private String figi;
    private int bestPeriod;
    private double bestScore;
    private int trials;
    private String description;

    public OptimizationResultDto() {
    }

    public OptimizationResultDto(String figi, int bestPeriod, double bestScore, int trials, String description) {
        this.figi = figi;
        this.bestPeriod = bestPeriod;
        this.bestScore = bestScore;
        this.trials = trials;
        this.description = description;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public int getBestPeriod() {
        return bestPeriod;
    }

    public void setBestPeriod(int bestPeriod) {
        this.bestPeriod = bestPeriod;
    }

    public double getBestScore() {
        return bestScore;
    }

    public void setBestScore(double bestScore) {
        this.bestScore = bestScore;
    }

    public int getTrials() {
        return trials;
    }

    public void setTrials(int trials) {
        this.trials = trials;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
