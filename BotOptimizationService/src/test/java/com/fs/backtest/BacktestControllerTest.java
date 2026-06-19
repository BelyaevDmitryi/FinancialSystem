package com.fs.backtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.exception.ExceptionController;
import com.fs.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BacktestController.class)
@Import(ExceptionController.class)
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.client.hostname=localhost",
        "spring.cloud.client.ip-address=127.0.0.1"
})
class BacktestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BacktestService backtestService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void run_returnsMetrics() throws Exception {
        BacktestRunRequest request = new BacktestRunRequest();
        request.setFigi("BBG004730N88");
        request.setFrom(Instant.parse("2026-01-01T00:00:00Z"));
        request.setTo(Instant.parse("2026-03-01T00:00:00Z"));
        request.setSmaPeriod(10);
        request.setInitialCash(BigDecimal.valueOf(50_000));

        BacktestResultDto response = new BacktestResultDto();
        response.setTrades(4);
        response.setTotalReturn(BigDecimal.valueOf(0.05));
        response.setFinalEquity(BigDecimal.valueOf(52_500));

        when(backtestService.run(any())).thenReturn(response);

        mockMvc.perform(post("/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trades").value(4))
                .andExpect(jsonPath("$.totalReturn").value(0.05));
    }
}
