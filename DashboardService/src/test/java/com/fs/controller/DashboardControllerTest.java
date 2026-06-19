package com.fs.controller;

import com.fs.config.DashboardProperties;
import com.fs.config.TestSecurityConfig;
import com.fs.dto.JournalPositionDto;
import com.fs.dto.PriceDataDto;
import com.fs.dto.StockDto;
import com.fs.feignclient.JournalClient;
import com.fs.feignclient.MarketHistoryClient;
import com.fs.feignclient.PriceServiceClient;
import com.fs.feignclient.TradingBotServiceClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.security.JwtAuthenticationFilter;
import com.fs.service.DailyChangeCalculator;
import com.fs.service.DashboardService;
import com.fs.service.PriceBaselineService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({DashboardService.class, DailyChangeCalculator.class, PriceBaselineService.class, TestSecurityConfig.class})
@EnableConfigurationProperties(DashboardProperties.class)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.client.hostname=localhost",
        "spring.cloud.client.ip-address=127.0.0.1",
        "dashboard.price-baseline=REDIS_SNAPSHOT"
})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JournalClient journalClient;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private PriceServiceClient priceServiceClient;

    @MockBean
    private MarketHistoryClient marketHistoryClient;

    @MockBean
    private TradingTerminalServiceClient terminalServiceClient;

    @MockBean
    private TradingBotServiceClient botServiceClient;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void configureJwtFilterPassThrough() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    (HttpServletRequest) inv.getArgument(0),
                    (HttpServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void getUserDashboard_usesJournalPositionsAndDailyChange() throws Exception {
        when(journalClient.getPositions("42")).thenReturn(List.of(
                new JournalPositionDto(42L, "BBG004730N88", BigDecimal.TEN, BigDecimal.valueOf(150))
        ));
        when(userServiceClient.getStocksByFigis(any())).thenReturn(List.of(
                new StockDto("BBG004730N88", "SBER", "Сбербанк", "RUB")
        ));
        when(priceServiceClient.getPrices(List.of("BBG004730N88"))).thenReturn(List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(110), null)
        ));
        when(priceServiceClient.getSnapshotPrices(List.of("BBG004730N88"))).thenReturn(List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(100), null)
        ));
        when(terminalServiceClient.getUserOrders("42")).thenReturn(List.of());
        when(botServiceClient.getUserBots("42")).thenReturn(List.of());

        mockMvc.perform(get("/dashboard").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("42"))
                .andExpect(jsonPath("$.assets[0].figi").value("BBG004730N88"))
                .andExpect(jsonPath("$.assets[0].ticker").value("SBER"))
                .andExpect(jsonPath("$.assets[0].name").value("Сбербанк"))
                .andExpect(jsonPath("$.assets[0].quantity").value(10))
                .andExpect(jsonPath("$.dailyChange").value(100))
                .andExpect(jsonPath("$.dailyChangePercent").value(10.0));
    }
}
