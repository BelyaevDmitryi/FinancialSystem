package com.fs.service;

import com.fs.domain.BrokerOrderType;
import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.PriceServiceClient;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePaperTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BrokerIntegrationServiceClient brokerIntegrationServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private JournalFillPublisher journalFillPublisher;

    @Mock
    private PriceServiceClient priceServiceClient;

    @InjectMocks
    private PaperFillSimulator paperFillSimulator;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                brokerIntegrationServiceClient,
                userServiceClient,
                journalFillPublisher,
                paperFillSimulator
        );
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

    @Test
    void paperOrder_skipsBrokerAndPublishesFillAtMarketPrice() {
        stubSaveWithId(1L);

        BigDecimal marketPrice = BigDecimal.valueOf(250.50);
        when(priceServiceClient.getPrices(List.of("BBG004730N88")))
                .thenReturn(List.of(new PriceDataDto("BBG004730N88", marketPrice, LocalDateTime.now())));

        CreateOrderDto request = paperOrderRequest();

        OrderDto result = orderService.createOrder("42", request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(result.getBrokerOrderId()).isNull();
        assertThat(result.isPaper()).isTrue();
        assertThat(result.getPrice()).isEqualByComparingTo(marketPrice);

        verifyNoInteractions(brokerIntegrationServiceClient);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(journalFillPublisher).publishFill(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(orderCaptor.getValue().isPaper()).isTrue();
    }

    @Test
    void liveOrder_stillCallsBroker() {
        stubSaveWithId(2L);

        CreateOrderDto request = paperOrderRequest();
        request.setPaper(false);

        when(brokerIntegrationServiceClient.placeOrder(eq("test-account"), any(), any()))
                .thenReturn(brokerFillResponse());

        OrderDto result = orderService.createOrder("42", request);

        assertThat(result.isPaper()).isFalse();
        verify(brokerIntegrationServiceClient).placeOrder(eq("test-account"), any(), any());
        verify(priceServiceClient, never()).getPrices(any());
    }

    private static CreateOrderDto paperOrderRequest() {
        CreateOrderDto request = new CreateOrderDto();
        request.setFigi("BBG004730N88");
        request.setType(OrderType.BUY);
        request.setQuantity(BigDecimal.ONE);
        request.setPrice(BigDecimal.valueOf(100));
        request.setOrderType(BrokerOrderType.MARKET);
        request.setComment("paper-test");
        request.setPaper(true);
        return request;
    }

    private static com.fs.dto.BrokerOrderDto brokerFillResponse() {
        com.fs.dto.BrokerOrderDto dto = new com.fs.dto.BrokerOrderDto();
        dto.setOrderId("MOCK-P1");
        dto.setStatus("FILL");
        dto.setFigi("BBG004730N88");
        return dto;
    }
}
