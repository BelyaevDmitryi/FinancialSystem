package com.fs.dto;

import com.fs.domain.ImportJobStatus;

import java.time.Instant;

public class ImportJobStatusDto {

    private Long jobId;
    private ImportJobStatus status;
    private String figi;
    private String interval;
    private Instant from;
    private Instant to;
    private int insertedCount;
    private int updatedCount;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;

    public ImportJobStatusDto() {
    }

    public ImportJobStatusDto(
            Long jobId,
            ImportJobStatus status,
            String figi,
            String interval,
            Instant from,
            Instant to,
            int insertedCount,
            int updatedCount,
            String errorMessage,
            Instant createdAt,
            Instant completedAt) {
        this.jobId = jobId;
        this.status = status;
        this.figi = figi;
        this.interval = interval;
        this.from = from;
        this.to = to;
        this.insertedCount = insertedCount;
        this.updatedCount = updatedCount;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public ImportJobStatus getStatus() {
        return status;
    }

    public void setStatus(ImportJobStatus status) {
        this.status = status;
    }

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

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
