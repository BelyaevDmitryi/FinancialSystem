package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "import_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String figi;

    @Column(name = "candle_interval", nullable = false, length = 32)
    private String interval;

    @Column(name = "from_time", nullable = false)
    private Instant fromTime;

    @Column(name = "to_time", nullable = false)
    private Instant toTime;

    @Column(length = 64)
    private String broker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportJobStatus status;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ImportJobStatus.PENDING;
        }
    }
}
