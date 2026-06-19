package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String figi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeSide side;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "realized_pnl")
    private BigDecimal realizedPnl;

    /** Terminal order id — idempotency key for fill ingest (ADR-002 §4). */
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    private BigDecimal commission;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
}
