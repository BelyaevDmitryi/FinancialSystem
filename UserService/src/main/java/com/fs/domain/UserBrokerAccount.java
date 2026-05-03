package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Счёт пользователя у брокера (элемент списка счетов в сущности UserBroker).
 * Связь: UserBroker 1 : N UserBrokerAccount.
 */
@Entity
@Table(name = "user_broker_accounts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_broker_id", "account_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBrokerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_broker_id", nullable = false)
    private UserBroker userBroker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
