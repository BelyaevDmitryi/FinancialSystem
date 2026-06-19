package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String figi;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;
    
    @Column(nullable = false)
    private BigDecimal quantity;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime executedAt;
    private String comment;

    @Column(name = "broker_order_id")
    private String brokerOrderId;

    @Column(name = "broker_code")
    private String brokerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private BrokerOrderType orderType;

    @Column(name = "stop_price")
    private BigDecimal stopPrice;

    @Column(name = "broker_status")
    private String brokerStatus;

    @Column(nullable = false)
    private boolean paper = false;
}
