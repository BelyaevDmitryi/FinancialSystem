package com.fs.dto;

import com.fs.domain.BrokerOrderType;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String id;
    private String userId;
    private String figi;
    private OrderType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
    private String comment;
    private String brokerOrderId;
    private String brokerCode;
    private BrokerOrderType orderType;
    private BigDecimal stopPrice;
    private String brokerStatus;
    private boolean paper;
}
