package com.fs.service;

import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.repository.OrderRepository;
import com.fs.support.TestFeignConfig;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "docker.tests", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestFeignConfig.class)
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrder_brokerDisabled_savesPendingOrder() {
        // given — broker.integration.enabled=false в application-test.yml
        CreateOrderDto request = new CreateOrderDto(
                "BBG004730N88", OrderType.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(150), "test comment");

        // when
        OrderDto result = orderService.createOrder("42", request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getFigi()).isEqualTo("BBG004730N88");
        assertThat(result.getUserId()).isEqualTo("42");

        // Проверяем в БД
        long count = orderRepository.findByUserId(42L).stream()
                .filter(o -> o.getFigi().equals("BBG004730N88"))
                .count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
