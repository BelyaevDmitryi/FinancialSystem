package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.dto.BrokerOrderDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSyncSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BrokerIntegrationServiceClient brokerIntegrationServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private JournalFillPublisher journalFillPublisher;

    @InjectMocks
    private OrderSyncService orderSyncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderSyncService, "brokerIntegrationEnabled", true);
        ReflectionTestUtils.setField(orderSyncService, "defaultAccountId", "test-account");
        ReflectionTestUtils.setField(orderSyncService, "defaultBrokerCode", "MOCK");
    }

    @Test
    void syncPendingOrders_brokerFill_executesOrderLocally() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setBrokerOrderId("MOCK-1");

        when(orderRepository.findByStatusAndBrokerOrderIdIsNotNull(OrderStatus.PENDING))
                .thenReturn(List.of(order));

        BrokerOrderDto brokerOrder = new BrokerOrderDto();
        brokerOrder.setOrderId("MOCK-1");
        brokerOrder.setStatus("FILL");
        when(brokerIntegrationServiceClient.getOrderStatus(eq("test-account"), eq("MOCK-1"), eq(null)))
                .thenReturn(brokerOrder);

        orderSyncService.syncPendingOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(order.getExecutedAt()).isNotNull();
        assertThat(order.getBrokerStatus()).isEqualTo("FILL");
        verify(orderRepository).save(order);
        verify(journalFillPublisher).publishFill(order);
    }

    @Test
    void syncPendingOrders_brokerCancelled_cancelsOrderLocally() {
        Order order = new Order();
        order.setId(2L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setBrokerOrderId("MOCK-2");

        when(orderRepository.findByStatusAndBrokerOrderIdIsNotNull(OrderStatus.PENDING))
                .thenReturn(List.of(order));

        BrokerOrderDto brokerOrder = new BrokerOrderDto();
        brokerOrder.setOrderId("MOCK-2");
        brokerOrder.setStatus("CANCELLED");
        when(brokerIntegrationServiceClient.getOrderStatus(eq("test-account"), eq("MOCK-2"), eq(null)))
                .thenReturn(brokerOrder);

        orderSyncService.syncPendingOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getBrokerStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(order);
    }

    @Test
    void syncOrder_alreadyExecuted_doesNotPublishFillAgain() {
        Order order = new Order();
        order.setId(3L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setBrokerOrderId("MOCK-3");

        BrokerOrderDto brokerOrder = new BrokerOrderDto();
        brokerOrder.setOrderId("MOCK-3");
        brokerOrder.setStatus("FILL");
        when(brokerIntegrationServiceClient.getOrderStatus(eq("test-account"), eq("MOCK-3"), eq(null)))
                .thenReturn(brokerOrder);

        orderSyncService.syncOrder(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        verify(journalFillPublisher).publishFill(order);

        orderSyncService.syncOrder(order);

        verify(journalFillPublisher, times(1)).publishFill(order);
    }
}
