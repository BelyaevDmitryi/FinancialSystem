package com.fs.service;

import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.repository.OrderRepository;
import com.fs.support.TestFeignConfig;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "docker.tests", matches = "true")
@SpringBootTest(properties = {"broker.integration.enabled=true"})
@ActiveProfiles("test")
@Import(TestFeignConfig.class)
@Transactional
class OrderServiceBrokerEnabledIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private BrokerIntegrationServiceClient brokerClient;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    void createOrder_brokerEnabled_callsBrokerAndSavesBrokerOrderId() {
        // given
        BrokerOrderDto mockBrokerResponse = new BrokerOrderDto();
        mockBrokerResponse.setOrderId("MOCK-999");
        mockBrokerResponse.setStatus("FILL");
        mockBrokerResponse.setFigi("BBG004730N88");

        when(brokerClient.placeOrder(anyString(), any(), any())).thenReturn(mockBrokerResponse);

        // account id из application-test.yml: broker.default-account-id=test-account
        CreateOrderDto request = new CreateOrderDto(
                "BBG004730N88", OrderType.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(150), null);

        // when
        OrderDto result = orderService.createOrder("42", request);

        // then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(result.getBrokerOrderId()).isEqualTo("MOCK-999");
        assertThat(result.getBrokerStatus()).isEqualTo("FILL");
    }
}
