package com.fs.service;

import com.fs.domain.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrokerOrderIdResolverTest {

    @Test
    void resolve_returnsBrokerOrderIdFromColumn() {
        Order order = new Order();
        order.setBrokerOrderId("MOCK-42");

        assertThat(BrokerOrderIdResolver.resolve(order)).isEqualTo("MOCK-42");
    }

    @Test
    void resolve_returnsNullWhenBrokerOrderIdMissingOrBlank() {
        Order nullId = new Order();
        nullId.setBrokerOrderId(null);

        Order emptyId = new Order();
        emptyId.setBrokerOrderId("");

        Order blankId = new Order();
        blankId.setBrokerOrderId("   ");

        assertThat(BrokerOrderIdResolver.resolve(nullId)).isNull();
        assertThat(BrokerOrderIdResolver.resolve(emptyId)).isNull();
        assertThat(BrokerOrderIdResolver.resolve(blankId)).isNull();
    }
}
