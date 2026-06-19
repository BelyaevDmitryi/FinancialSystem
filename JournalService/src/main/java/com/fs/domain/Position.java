package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "positions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_positions_user_figi", columnNames = {"user_id", "figi"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String figi;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "avg_price", nullable = false)
    private BigDecimal avgPrice;
}
