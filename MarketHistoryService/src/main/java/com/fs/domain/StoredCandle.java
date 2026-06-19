package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "candles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String figi;

    @Column(name = "candle_interval", nullable = false, length = 32)
    private String interval;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private BigDecimal open;

    @Column(nullable = false)
    private BigDecimal high;

    @Column(nullable = false)
    private BigDecimal low;

    @Column(nullable = false)
    private BigDecimal close;

    @Column(nullable = false)
    private long volume;
}
