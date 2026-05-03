package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Связка пользователь ↔ брокер (users 1 : N brokers).
 * Содержит список счетов пользователя у этого брокера и выбранный счёт по умолчанию.
 */
@Entity
@Table(name = "user_brokers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "broker_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBroker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    private Broker broker;

    /** Список счетов пользователя у этого брокера. */
    @OneToMany(mappedBy = "userBroker", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserBrokerAccount> accounts = new ArrayList<>();

    /** Выбранный счёт из списка (для торговли по умолчанию). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_account_id")
    private Account defaultAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
