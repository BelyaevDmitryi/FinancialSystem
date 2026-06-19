package com.fs.service;

import com.fs.domain.BrokerOrderType;
import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.AmendOrderDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BrokerIntegrationServiceClient brokerIntegrationServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private JournalFillPublisher journalFillPublisher;

    @Mock
    private PaperFillSimulator paperFillSimulator;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "brokerIntegrationEnabled", false);
        ReflectionTestUtils.setField(orderService, "defaultAccountId", "test-account");
        ReflectionTestUtils.setField(orderService, "defaultBrokerCode", "MOCK");
    }

    @Test
    void createOrder_marketOrderType_persisted() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        CreateOrderDto request = new CreateOrderDto();
        request.setFigi("BBG004730N88");
        request.setType(OrderType.BUY);
        request.setQuantity(BigDecimal.ONE);
        request.setPrice(BigDecimal.valueOf(100));
        request.setOrderType(BrokerOrderType.MARKET);

        OrderDto result = orderService.createOrder("42", request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo(BrokerOrderType.MARKET);
        assertThat(result.getOrderType()).isEqualTo(BrokerOrderType.MARKET);
    }

    @Test
    void createOrder_limitOrderType_persisted() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(2L);
            return order;
        });

        CreateOrderDto request = new CreateOrderDto();
        request.setFigi("BBG004730N88");
        request.setType(OrderType.SELL);
        request.setQuantity(BigDecimal.TEN);
        request.setPrice(BigDecimal.valueOf(105));
        request.setOrderType(BrokerOrderType.LIMIT);

        orderService.createOrder("42", request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo(BrokerOrderType.LIMIT);
    }

    @Test
    void createOrder_stopOrderType_persistedWithStopPrice() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(3L);
            return order;
        });

        CreateOrderDto request = new CreateOrderDto();
        request.setFigi("BBG004730N88");
        request.setType(OrderType.SELL);
        request.setQuantity(BigDecimal.ONE);
        request.setPrice(BigDecimal.valueOf(100));
        request.setOrderType(BrokerOrderType.STOP);
        request.setStopPrice(BigDecimal.valueOf(95));

        orderService.createOrder("42", request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo(BrokerOrderType.STOP);
        assertThat(captor.getValue().getStopPrice()).isEqualByComparingTo("95");
    }

    @Test
    void createOrder_nullOrderType_defaultsToLimit() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(4L);
            return order;
        });

        CreateOrderDto request = new CreateOrderDto();
        request.setFigi("BBG004730N88");
        request.setType(OrderType.BUY);
        request.setQuantity(BigDecimal.ONE);
        request.setPrice(BigDecimal.valueOf(100));

        orderService.createOrder("42", request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo(BrokerOrderType.LIMIT);
    }

    @Test
    void cancelOrder_brokerEnabled_cancelsOnBrokerAndSetsBrokerStatus() {
        ReflectionTestUtils.setField(orderService, "brokerIntegrationEnabled", true);

        Order order = new Order();
        order.setId(10L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setBrokerOrderId("MOCK-10");
        order.setBrokerCode("MOCK");
        order.setOrderType(BrokerOrderType.LIMIT);

        when(orderRepository.findById(10L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BrokerOrderDto cancelled = new BrokerOrderDto();
        cancelled.setOrderId("MOCK-10");
        cancelled.setStatus("CANCELLED");
        when(brokerIntegrationServiceClient.getOrderStatus(eq("test-account"), eq("MOCK-10"), eq("MOCK")))
                .thenReturn(cancelled);

        OrderDto result = orderService.cancelOrder("10", "42");

        verify(brokerIntegrationServiceClient).cancelOrder(eq("test-account"), eq("MOCK-10"), eq("MOCK"));
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getBrokerStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_brokerFails_keepsPendingAndThrows() {
        ReflectionTestUtils.setField(orderService, "brokerIntegrationEnabled", true);

        Order order = new Order();
        order.setId(12L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setBrokerOrderId("MOCK-12");
        order.setBrokerCode("MOCK");

        when(orderRepository.findById(12L)).thenReturn(java.util.Optional.of(order));
        doThrow(new RuntimeException("broker unavailable"))
                .when(brokerIntegrationServiceClient)
                .cancelOrder(eq("test-account"), eq("MOCK-12"), eq("MOCK"));

        assertThatThrownBy(() -> orderService.cancelOrder("12", "42"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("broker unavailable");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_paper_skipsBroker() {
        ReflectionTestUtils.setField(orderService, "brokerIntegrationEnabled", true);

        Order order = new Order();
        order.setId(13L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setPaper(true);
        order.setBrokerOrderId("MOCK-13");

        when(orderRepository.findById(13L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.cancelOrder("13", "42");

        verify(brokerIntegrationServiceClient, never()).cancelOrder(any(), any(), any());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getBrokerStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsConflict() {
        Order order = new Order();
        order.setId(14L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(14L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("14", "42"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Невозможно отменить");
    }

    @Test
    void amendOrder_limitPending_updatesPriceAndBroker() {
        ReflectionTestUtils.setField(orderService, "brokerIntegrationEnabled", true);

        Order order = new Order();
        order.setId(11L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setBrokerOrderId("MOCK-11");
        order.setOrderType(BrokerOrderType.LIMIT);
        order.setPrice(BigDecimal.valueOf(100));

        when(orderRepository.findById(11L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BrokerOrderDto amended = new BrokerOrderDto();
        amended.setOrderId("MOCK-11");
        amended.setStatus("NEW");
        amended.setPrice(BigDecimal.valueOf(110));
        when(brokerIntegrationServiceClient.amendOrder(
                eq("test-account"), eq("MOCK-11"), any(AmendBrokerOrderDto.class), eq(null)))
                .thenReturn(amended);

        AmendOrderDto amendRequest = new AmendOrderDto();
        amendRequest.setPrice(BigDecimal.valueOf(110));

        OrderDto result = orderService.amendOrder("11", "42", amendRequest);

        assertThat(result.getPrice()).isEqualByComparingTo("110");
        assertThat(result.getBrokerStatus()).isEqualTo("NEW");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
