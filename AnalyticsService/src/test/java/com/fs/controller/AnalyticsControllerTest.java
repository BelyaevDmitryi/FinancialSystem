package com.fs.controller;

import com.fs.dto.*;
import com.fs.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnalyticsController.class)
@Import(com.fs.exception.ExceptionController.class)
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.client.hostname=localhost",
        "spring.cloud.client.ip-address=127.0.0.1"
})
@DisplayName("AnalyticsController")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    private static final String FIGI = "BBG004730N88";
    private static final int PERIOD = 5;
    private static final String VALID_REQUEST_BODY = """
            {
                "figi": "%s",
                "period": %d
            }
            """.formatted(FIGI, PERIOD);

    @Nested
    @DisplayName("POST /analytics/sma")
    class CalculateSMA {

        @Test
        @DisplayName("возвращает 200 и SMA при валидном запросе")
        void shouldReturnSma() throws Exception {
            SmaResponseDto response = new SmaResponseDto(
                    FIGI,
                    BigDecimal.valueOf(108),
                    LocalDateTime.now(),
                    PERIOD,
                    List.of(BigDecimal.valueOf(105), BigDecimal.valueOf(106), BigDecimal.valueOf(108))
            );
            when(analyticsService.calculateSMA(any(AnalyticsRequestDto.class))).thenReturn(response);

            mockMvc.perform(post("/analytics/sma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.figi").value(FIGI))
                    .andExpect(jsonPath("$.sma").value(108))
                    .andExpect(jsonPath("$.period").value(PERIOD))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.values").isArray());
        }
    }

    @Nested
    @DisplayName("POST /analytics/ema")
    class CalculateEMA {

        @Test
        @DisplayName("возвращает 200 и EMA при валидном запросе")
        void shouldReturnEma() throws Exception {
            EmaResponseDto response = new EmaResponseDto(
                    FIGI,
                    BigDecimal.valueOf(107.5),
                    LocalDateTime.now(),
                    PERIOD,
                    List.of(BigDecimal.valueOf(106), BigDecimal.valueOf(107.5))
            );
            when(analyticsService.calculateEMA(any(AnalyticsRequestDto.class))).thenReturn(response);

            mockMvc.perform(post("/analytics/ema")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.figi").value(FIGI))
                    .andExpect(jsonPath("$.ema").value(107.5))
                    .andExpect(jsonPath("$.period").value(PERIOD))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.values").isArray());
        }
    }

    @Nested
    @DisplayName("POST /analytics/volatility")
    class CalculateVolatility {

        @Test
        @DisplayName("возвращает 200 и волатильность при валидном запросе")
        void shouldReturnVolatility() throws Exception {
            VolatilityResponseDto response = new VolatilityResponseDto(
                    FIGI,
                    BigDecimal.valueOf(2.5),
                    PERIOD,
                    BigDecimal.valueOf(2.7)
            );
            when(analyticsService.calculateVolatility(any(AnalyticsRequestDto.class))).thenReturn(response);

            mockMvc.perform(post("/analytics/volatility")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.figi").value(FIGI))
                    .andExpect(jsonPath("$.volatility").value(2.5))
                    .andExpect(jsonPath("$.period").value(PERIOD))
                    .andExpect(jsonPath("$.standardDeviation").value(2.7));
        }
    }

    @Nested
    @DisplayName("POST /analytics/macd")
    class CalculateMACD {

        @Test
        @DisplayName("возвращает 200 и MACD при валидном запросе")
        void shouldReturnMacd() throws Exception {
            MacdResponseDto response = new MacdResponseDto(
                    FIGI,
                    BigDecimal.valueOf(0.5),
                    BigDecimal.valueOf(0.3),
                    BigDecimal.valueOf(0.2),
                    LocalDateTime.now()
            );
            when(analyticsService.calculateMACD(any(AnalyticsRequestDto.class))).thenReturn(response);

            mockMvc.perform(post("/analytics/macd")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.figi").value(FIGI))
                    .andExpect(jsonPath("$.macd").value(0.5))
                    .andExpect(jsonPath("$.signal").value(0.3))
                    .andExpect(jsonPath("$.histogram").value(0.2))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Валидация запроса")
    class Validation {

        @Test
        @DisplayName("400 при пустом FIGI")
        void shouldReturn400WhenFigiBlank() throws Exception {
            String body = """
                    {
                        "figi": "",
                        "period": 5
                    }
                    """;

            mockMvc.perform(post("/analytics/sma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 при отсутствии FIGI")
        void shouldReturn400WhenFigiMissing() throws Exception {
            String body = """
                    {
                        "period": 5
                    }
                    """;

            mockMvc.perform(post("/analytics/sma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 при period = null")
        void shouldReturn400WhenPeriodNull() throws Exception {
            String body = """
                    {
                        "figi": "BBG004730N88"
                    }
                    """;

            mockMvc.perform(post("/analytics/sma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 при period <= 0")
        void shouldReturn400WhenPeriodNotPositive() throws Exception {
            String body = """
                    {
                        "figi": "BBG004730N88",
                        "period": 0
                    }
                    """;

            mockMvc.perform(post("/analytics/sma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("Запрос с priceData")
    class RequestWithPriceData {

        @Test
        @DisplayName("принимает запрос с массивом priceData и передаёт в сервис")
        void shouldAcceptPriceDataInRequest() throws Exception {
            String body = """
                    {
                        "figi": "BBG004730N88",
                        "period": 5,
                        "priceData": [
                            {"figi": "BBG004730N88", "price": 100, "timestamp": "2025-01-01T12:00:00"},
                            {"figi": "BBG004730N88", "price": 101, "timestamp": "2025-01-02T12:00:00"},
                            {"figi": "BBG004730N88", "price": 102, "timestamp": "2025-01-03T12:00:00"},
                            {"figi": "BBG004730N88", "price": 103, "timestamp": "2025-01-04T12:00:00"},
                            {"figi": "BBG004730N88", "price": 104, "timestamp": "2025-01-05T12:00:00"}
                        ]
                    }
                    """;
            SmaResponseDto response = new SmaResponseDto(FIGI, BigDecimal.valueOf(102), LocalDateTime.now(), PERIOD,
                    List.of(BigDecimal.valueOf(101), BigDecimal.valueOf(102)));
            when(analyticsService.calculateSMA(any(AnalyticsRequestDto.class))).thenReturn(response);

            mockMvc.perform(post("/analytics/sma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sma").value(102));
        }
    }
}
