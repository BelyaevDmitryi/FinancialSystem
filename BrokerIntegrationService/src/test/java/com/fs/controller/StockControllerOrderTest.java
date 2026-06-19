package com.fs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.service.BrokerIntegrationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.client.hostname=localhost",
        "spring.cloud.client.ip-address=127.0.0.1",
        "spring.profiles.active=test"
})
class StockControllerOrderTest {

    private static final String ACCOUNT = "acc-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BrokerIntegrationService brokerIntegrationService;

    @Nested
    class PlaceOrder {

        @Test
        void placeOrder_market_returns201() throws Exception {
            stubPlaceOrder("MARKET", "FILL");
            CreateBrokerOrderDto body = new CreateBrokerOrderDto(
                    "BBG004730N88", 1L, null, "BUY", "MARKET", null, null);

            mockMvc.perform(post("/broker/orders")
                            .header("X-Account-Id", ACCOUNT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderType").value("MARKET"))
                    .andExpect(jsonPath("$.status").value("FILL"));
        }

        @Test
        void placeOrder_limit_returns201() throws Exception {
            stubPlaceOrder("LIMIT", "NEW");
            CreateBrokerOrderDto body = new CreateBrokerOrderDto(
                    "BBG004730N88", 2L, BigDecimal.valueOf(99), "BUY", "LIMIT", null, null);

            mockMvc.perform(post("/broker/orders")
                            .header("X-Account-Id", ACCOUNT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderType").value("LIMIT"))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        void placeOrder_stop_returns201() throws Exception {
            stubPlaceOrder("STOP", "NEW");
            CreateBrokerOrderDto body = new CreateBrokerOrderDto(
                    "BBG004730N88", 1L, null, "SELL", "STOP", BigDecimal.valueOf(95), null);

            mockMvc.perform(post("/broker/orders")
                            .header("X-Account-Id", ACCOUNT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderType").value("STOP"))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void cancelOrder_returns204() throws Exception {
            doNothing().when(brokerIntegrationService).cancelOrder(eq(ACCOUNT), eq("broker-order-1"));

            mockMvc.perform(post("/broker/orders/broker-order-1/cancel")
                            .header("X-Account-Id", ACCOUNT))
                    .andExpect(status().isNoContent());

            verify(brokerIntegrationService).cancelOrder(eq(ACCOUNT), eq("broker-order-1"));
        }
    }

    private void stubPlaceOrder(String orderType, String status) {
        BrokerOrderDto response = new BrokerOrderDto(
                "broker-order-1",
                "BBG004730N88",
                1L,
                "FILL".equals(status) ? 1L : 0L,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                "BUY",
                status,
                orderType,
                Instant.now(),
                "FILL".equals(status) ? Instant.now() : null,
                "ok"
        );
        when(brokerIntegrationService.placeOrder(eq(ACCOUNT), any(CreateBrokerOrderDto.class)))
                .thenReturn(response);
    }
}
