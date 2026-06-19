package com.fs.service;

import com.fs.domain.Order;

/**
 * Определение ID заявки у брокера из колонки {@code broker_order_id}.
 */
final class BrokerOrderIdResolver {

    private BrokerOrderIdResolver() {
    }

    static String resolve(Order order) {
        String brokerOrderId = order.getBrokerOrderId();
        if (brokerOrderId == null || brokerOrderId.isBlank()) {
            return null;
        }
        return brokerOrderId;
    }
}
