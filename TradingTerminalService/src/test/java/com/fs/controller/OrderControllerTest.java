package com.fs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.config.TestSecurityConfig;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.security.JwtAuthenticationFilter;
import com.fs.security.JwtUtils;
import com.fs.service.OrderService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void configureJwtFilterPassThrough() throws Exception {
        // Пробрасываем запрос через цепочку без JWT-проверки в тестах
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (HttpServletRequest) inv.getArgument(0),
                    (HttpServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Nested
    class CreateOrder {

        @Test
        void createOrder_validRequest_returns201() throws Exception {
            // given
            CreateOrderDto request = new CreateOrderDto("BBG004730N88", OrderType.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(100), null);
            OrderDto response = new OrderDto("1", "42", "BBG004730N88", OrderType.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(100), OrderStatus.PENDING,
                    LocalDateTime.now(), null, null,
                    null, null, null, null, null, false);
            when(orderService.createOrder(eq("42"), any(CreateOrderDto.class))).thenReturn(response);

            // when / then
            mockMvc.perform(post("/orders")
                            .header("X-User-Id", "42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.figi").value("BBG004730N88"));
        }

        @Test
        void createOrder_missingFigi_returns400() throws Exception {
            // given — figi пустой (нарушает @NotBlank)
            CreateOrderDto request = new CreateOrderDto("", OrderType.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(100), null);

            // when / then
            mockMvc.perform(post("/orders")
                            .header("X-User-Id", "42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createOrder_missingXUserId_returns4xx() throws Exception {
            // given — нет X-User-Id header
            CreateOrderDto request = new CreateOrderDto("BBG004730N88", OrderType.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(100), null);

            // when / then — отсутствие обязательного заголовка возвращает 400
            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }
    }
}
