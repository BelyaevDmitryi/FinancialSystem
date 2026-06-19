package com.fs.service;

import com.fs.domain.BrokerOrderType;
import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.AmendOrderDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Broker-enabled flow: MARKET/LIMIT/STOP placement и amend LIMIT без Docker (моки Feign).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceBrokerFlowTest {

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
        ReflectionTestUtils.setField(orderService, "brokerIntegrationEnabled", true);
        ReflectionTestUtils.setField(orderService, "defaultAccountId", "test-account");
        ReflectionTestUtils.setField(orderService, "defaultBrokerCode", "MOCK");
    }

    private void stubSaveWithId(long id) {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(id);
            }
            return order;
        });
    }

    @Nested
    class PlaceOrder {

        @Test
        void marketOrder_brokerFill_returnsExecutedWithBrokerOrderId() {
            stubSaveWithId(1L);

            BrokerOrderDto brokerResponse = brokerOrder("MOCK-M1", "FILL");
            when(brokerIntegrationServiceClient.placeOrder(eq("test-account"), any(CreateBrokerOrderDto.class), isNull()))
                    .thenReturn(brokerResponse);

            CreateOrderDto request = orderRequest(BrokerOrderType.MARKET, null);

            OrderDto result = orderService.createOrder("42", request);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.EXECUTED);
            assertThat(result.getBrokerOrderId()).isEqualTo("MOCK-M1");
            assertThat(result.getBrokerStatus()).isEqualTo("FILL");
            assertThat(result.getOrderType()).isEqualTo(BrokerOrderType.MARKET);

            ArgumentCaptor<CreateBrokerOrderDto> captor = ArgumentCaptor.forClass(CreateBrokerOrderDto.class);
            verify(brokerIntegrationServiceClient).placeOrder(eq("test-account"), captor.capture(), isNull());
            assertThat(captor.getValue().getOrderType()).isEqualTo("MARKET");

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(journalFillPublisher).publishFill(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.EXECUTED);
        }

        @Test
        void limitOrder_brokerNew_staysPendingWithBrokerOrderId() {
            stubSaveWithId(2L);

            BrokerOrderDto brokerResponse = brokerOrder("MOCK-L1", "NEW");
            when(brokerIntegrationServiceClient.placeOrder(eq("test-account"), any(CreateBrokerOrderDto.class), isNull()))
                    .thenReturn(brokerResponse);

            CreateOrderDto request = orderRequest(BrokerOrderType.LIMIT, null);

            OrderDto result = orderService.createOrder("42", request);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getBrokerOrderId()).isEqualTo("MOCK-L1");
            assertThat(result.getBrokerStatus()).isEqualTo("NEW");

            ArgumentCaptor<CreateBrokerOrderDto> captor = ArgumentCaptor.forClass(CreateBrokerOrderDto.class);
            verify(brokerIntegrationServiceClient).placeOrder(eq("test-account"), captor.capture(), isNull());
            assertThat(captor.getValue().getOrderType()).isEqualTo("LIMIT");
        }

        @Test
        void stopOrder_brokerNew_persistsStopPriceAndStaysPending() {
            stubSaveWithId(3L);

            BrokerOrderDto brokerResponse = brokerOrder("MOCK-S1", "NEW");
            when(brokerIntegrationServiceClient.placeOrder(eq("test-account"), any(CreateBrokerOrderDto.class), isNull()))
                    .thenReturn(brokerResponse);

            CreateOrderDto request = orderRequest(BrokerOrderType.STOP, BigDecimal.valueOf(95));

            OrderDto result = orderService.createOrder("42", request);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getBrokerOrderId()).isEqualTo("MOCK-S1");
            assertThat(result.getOrderType()).isEqualTo(BrokerOrderType.STOP);
            assertThat(result.getStopPrice()).isEqualByComparingTo("95");

            ArgumentCaptor<CreateBrokerOrderDto> captor = ArgumentCaptor.forClass(CreateBrokerOrderDto.class);
            verify(brokerIntegrationServiceClient).placeOrder(eq("test-account"), captor.capture(), isNull());
            assertThat(captor.getValue().getOrderType()).isEqualTo("STOP");
            assertThat(captor.getValue().getStopPrice()).isEqualByComparingTo("95");
        }
    }

    @Nested
    class AmendLimit {

        @Test
        void amendLimitPending_updatesLocalAndBrokerPrice() {
            Order order = new Order();
            order.setId(11L);
            order.setUserId(42L);
            order.setStatus(OrderStatus.PENDING);
            order.setBrokerOrderId("MOCK-11");
            order.setOrderType(BrokerOrderType.LIMIT);
            order.setPrice(BigDecimal.valueOf(100));

            when(orderRepository.findById(11L)).thenReturn(java.util.Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            BrokerOrderDto amended = brokerOrder("MOCK-11", "NEW");
            amended.setPrice(BigDecimal.valueOf(110));
            when(brokerIntegrationServiceClient.amendOrder(
                    eq("test-account"), eq("MOCK-11"), any(AmendBrokerOrderDto.class), isNull()))
                    .thenReturn(amended);

            AmendOrderDto amendRequest = new AmendOrderDto();
            amendRequest.setPrice(BigDecimal.valueOf(110));

            OrderDto result = orderService.amendOrder("11", "42", amendRequest);

            assertThat(result.getPrice()).isEqualByComparingTo("110");
            assertThat(result.getBrokerStatus()).isEqualTo("NEW");
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(brokerIntegrationServiceClient).amendOrder(
                    eq("test-account"), eq("MOCK-11"), any(AmendBrokerOrderDto.class), isNull());
        }
    }

    private static CreateOrderDto orderRequest(BrokerOrderType orderType, BigDecimal stopPrice) {
        CreateOrderDto request = new CreateOrderDto();
        request.setFigi("BBG004730N88");
        request.setType(OrderType.BUY);
        request.setQuantity(BigDecimal.ONE);
        request.setPrice(BigDecimal.valueOf(100));
        request.setOrderType(orderType);
        request.setStopPrice(stopPrice);
        request.setComment("broker-flow-test");
        return request;
    }

    private static BrokerOrderDto brokerOrder(String orderId, String status) {
        BrokerOrderDto dto = new BrokerOrderDto();
        dto.setOrderId(orderId);
        dto.setStatus(status);
        dto.setFigi("BBG004730N88");
        return dto;
    }
}
