package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.dto.BrokerOrderDto;

import java.time.LocalDateTime;

/**
 * Сопоставление статуса заявки брокера с локальным ордером.
 */
final class BrokerOrderStatusMapper {

    private BrokerOrderStatusMapper() {
    }

    static void applyBrokerStatus(Order order, BrokerOrderDto brokerOrder) {
        if (brokerOrder == null || brokerOrder.getStatus() == null) {
            return;
        }
        String brokerStatus = brokerOrder.getStatus().trim().toUpperCase();
        order.setBrokerStatus(brokerStatus);
        switch (brokerStatus) {
            case "FILL", "DONE", "FILLED" -> {
                order.setStatus(OrderStatus.EXECUTED);
                order.setExecutedAt(LocalDateTime.now());
            }
            case "CANCELLED", "CANCEL" -> order.setStatus(OrderStatus.CANCELLED);
            case "REJECTED" -> order.setStatus(OrderStatus.REJECTED);
            default -> {
                // NEW, PARTIALLY_FILLED — локальный статус PENDING сохраняется
            }
        }
    }
}
