package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Счёт у брокера (external_account_id у конкретного брокера).
 * Связь: brokers 1 : N accounts (у брокера много счетов).
 */
@Entity
@Table(name = "accounts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"broker_id", "external_account_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    private Broker broker;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
