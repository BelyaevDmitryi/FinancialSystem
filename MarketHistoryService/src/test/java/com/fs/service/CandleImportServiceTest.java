package com.fs.service;

import com.fs.domain.ImportJob;
import com.fs.domain.ImportJobStatus;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.CandleImportRequest;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.ImportJobStatusDto;
import com.fs.feign.BrokerIntegrationClient;
import com.fs.repository.CandleRepository;
import com.fs.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandleImportServiceTest {

    private static final String FIGI = "BBG004730N88";
    private static final String INTERVAL = "DAY";
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-01-05T00:00:00Z");
    private static final Instant BAR1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant BAR2 = Instant.parse("2026-01-02T00:00:00Z");

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private BrokerIntegrationClient brokerIntegrationClient;

    @InjectMocks
    private CandleImportService candleImportService;

    private final AtomicLong jobIdSeq = new AtomicLong(1);

    @BeforeEach
    void stubJobSave() {
        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobIdSeq.getAndIncrement());
            }
            return job;
        });
    }

    @Test
    void startImport_createsPendingJobAndReturnsId() {
        when(importJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob(1L)));
        when(brokerIntegrationClient.getHistoricCandles(FIGI, FROM, TO, INTERVAL, null))
                .thenReturn(new HistoricCandlesDto(FIGI, INTERVAL, List.of()));

        Long jobId = candleImportService.startImport(request());

        assertThat(jobId).isEqualTo(1L);
        ArgumentCaptor<ImportJob> captor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(ImportJobStatus.PENDING);
    }

    @Test
    void runImport_fetchesBrokerAndCompletesWithCounts() {
        ImportJob pending = pendingJob(1L);
        when(importJobRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(brokerIntegrationClient.getHistoricCandles(FIGI, FROM, TO, INTERVAL, null))
                .thenReturn(new HistoricCandlesDto(FIGI, INTERVAL, List.of(bar(BAR1, 100), bar(BAR2, 101))));
        when(candleRepository.existsByFigiAndIntervalAndTime(eq(FIGI), eq(INTERVAL), any()))
                .thenReturn(false);

        candleImportService.runImport(1L);

        ImportJobStatusDto status = candleImportService.getJobStatus(1L);
        assertThat(status.getStatus()).isEqualTo(ImportJobStatus.COMPLETED);
        assertThat(status.getInsertedCount()).isEqualTo(2);
        assertThat(status.getUpdatedCount()).isEqualTo(0);
        verify(candleRepository, times(2)).upsert(any());
        verify(candleRepository).flush();
    }

    @Test
    void runImport_idempotentUpsertCountsUpdated() {
        ImportJob pending = pendingJob(2L);
        when(importJobRepository.findById(2L)).thenReturn(Optional.of(pending));
        when(brokerIntegrationClient.getHistoricCandles(FIGI, FROM, TO, INTERVAL, null))
                .thenReturn(new HistoricCandlesDto(FIGI, INTERVAL, List.of(bar(BAR1, 100))));
        when(candleRepository.existsByFigiAndIntervalAndTime(FIGI, INTERVAL, BAR1)).thenReturn(true);

        candleImportService.runImport(2L);

        ImportJobStatusDto status = candleImportService.getJobStatus(2L);
        assertThat(status.getInsertedCount()).isEqualTo(0);
        assertThat(status.getUpdatedCount()).isEqualTo(1);
    }

    @Test
    void runImport_marksJobCompletedWithTimestamp() {
        ImportJob pending = pendingJob(3L);
        when(importJobRepository.findById(3L)).thenReturn(Optional.of(pending));
        when(brokerIntegrationClient.getHistoricCandles(FIGI, FROM, TO, INTERVAL, null))
                .thenReturn(new HistoricCandlesDto(FIGI, INTERVAL, List.of()));

        candleImportService.runImport(3L);

        ImportJobStatusDto status = candleImportService.getJobStatus(3L);
        assertThat(status.getStatus()).isEqualTo(ImportJobStatus.COMPLETED);
        assertThat(status.getCompletedAt()).isNotNull();
        verify(importJobRepository, times(2)).save(any(ImportJob.class));
    }

    @Test
    void runImport_brokerFailureMarksJobFailed() {
        ImportJob pending = pendingJob(4L);
        when(importJobRepository.findById(4L)).thenReturn(Optional.of(pending));
        when(brokerIntegrationClient.getHistoricCandles(FIGI, FROM, TO, INTERVAL, null))
                .thenThrow(new RuntimeException("broker unavailable"));

        candleImportService.runImport(4L);

        ImportJobStatusDto status = candleImportService.getJobStatus(4L);
        assertThat(status.getStatus()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(status.getErrorMessage()).contains("broker unavailable");
        verify(candleRepository, never()).upsert(any());
    }

    private static CandleImportRequest request() {
        CandleImportRequest request = new CandleImportRequest();
        request.setFigi(FIGI);
        request.setFrom(FROM);
        request.setTo(TO);
        request.setInterval(INTERVAL);
        return request;
    }

    private static ImportJob pendingJob(long id) {
        ImportJob job = new ImportJob(
                id, FIGI, INTERVAL, FROM, TO, null,
                ImportJobStatus.PENDING, 0, 0, null,
                Instant.parse("2026-05-01T00:00:00Z"), null);
        return job;
    }

    private static BrokerCandleDto bar(Instant time, double close) {
        return new BrokerCandleDto(
                time,
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(close + 1),
                BigDecimal.valueOf(close - 1),
                BigDecimal.valueOf(close),
                1000L);
    }
}
