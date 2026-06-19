package com.fs.service;

import com.fs.domain.ImportJob;
import com.fs.domain.ImportJobStatus;
import com.fs.domain.StoredCandle;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.CandleImportRequest;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.ImportJobStatusDto;
import com.fs.exception.ImportJobNotFoundException;
import com.fs.feign.BrokerIntegrationClient;
import com.fs.repository.CandleRepository;
import com.fs.repository.ImportJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CandleImportService {

    private static final Logger log = LoggerFactory.getLogger(CandleImportService.class);

    private final ImportJobRepository importJobRepository;
    private final CandleRepository candleRepository;
    private final BrokerIntegrationClient brokerIntegrationClient;

    public CandleImportService(
            ImportJobRepository importJobRepository,
            CandleRepository candleRepository,
            BrokerIntegrationClient brokerIntegrationClient) {
        this.importJobRepository = importJobRepository;
        this.candleRepository = candleRepository;
        this.brokerIntegrationClient = brokerIntegrationClient;
    }

    @Transactional
    public Long startImport(CandleImportRequest request) {
        validateRange(request.getFrom(), request.getTo());
        ImportJob job = new ImportJob(
                null,
                request.getFigi(),
                request.getInterval(),
                request.getFrom(),
                request.getTo(),
                request.getBroker(),
                ImportJobStatus.PENDING,
                0,
                0,
                null,
                null,
                null);
        job = importJobRepository.save(job);
        Long jobId = job.getId();
        runImportAsync(jobId);
        return jobId;
    }

    @Async
    public void runImportAsync(Long jobId) {
        runImport(jobId);
    }

    /**
     * Синхронное выполнение импорта (используется async-обёрткой и тестами).
     */
    @Transactional
    public void runImport(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(jobId));
        if (job.getStatus() != ImportJobStatus.PENDING) {
            log.warn("Импорт jobId={} уже в статусе {}", jobId, job.getStatus());
            return;
        }
        job.setStatus(ImportJobStatus.RUNNING);
        importJobRepository.save(job);

        try {
            HistoricCandlesDto brokerData = brokerIntegrationClient.getHistoricCandles(
                    job.getFigi(), job.getFromTime(), job.getToTime(), job.getInterval(), job.getBroker());
            List<BrokerCandleDto> candles = brokerData.getCandles() != null
                    ? brokerData.getCandles()
                    : List.of();
            int inserted = 0;
            int updated = 0;
            for (BrokerCandleDto candle : candles) {
                if (candleRepository.existsByFigiAndIntervalAndTime(
                        job.getFigi(), job.getInterval(), candle.getTime())) {
                    updated++;
                } else {
                    inserted++;
                }
                candleRepository.upsert(toStoredCandle(job, candle));
            }
            candleRepository.flush();
            job.setInsertedCount(inserted);
            job.setUpdatedCount(updated);
            job.setStatus(ImportJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            importJobRepository.save(job);
            log.info("Импорт jobId={} завершён: inserted={}, updated={}", jobId, inserted, updated);
        } catch (Exception e) {
            log.error("Импорт jobId={} завершился с ошибкой", jobId, e);
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            importJobRepository.save(job);
        }
    }

    @Transactional(readOnly = true)
    public ImportJobStatusDto getJobStatus(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(jobId));
        return toStatusDto(job);
    }

    private static StoredCandle toStoredCandle(ImportJob job, BrokerCandleDto candle) {
        return new StoredCandle(
                null,
                job.getFigi(),
                job.getInterval(),
                candle.getTime(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume());
    }

    private static ImportJobStatusDto toStatusDto(ImportJob job) {
        return new ImportJobStatusDto(
                job.getId(),
                job.getStatus(),
                job.getFigi(),
                job.getInterval(),
                job.getFromTime(),
                job.getToTime(),
                job.getInsertedCount(),
                job.getUpdatedCount(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt());
    }

    private static void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Параметр from должен быть не позже to");
        }
    }
}
