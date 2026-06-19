package com.fs.service;

import com.fs.config.DashboardProperties;
import com.fs.dto.*;
import com.fs.feignclient.JournalClient;
import com.fs.feignclient.PriceServiceClient;
import com.fs.feignclient.TradingBotServiceClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.feignclient.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private JournalClient journalClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PriceServiceClient priceServiceClient;

    @Mock
    private PriceBaselineService priceBaselineService;

    @Mock
    private TradingTerminalServiceClient terminalServiceClient;

    @Mock
    private TradingBotServiceClient botServiceClient;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                journalClient,
                userServiceClient,
                priceServiceClient,
                priceBaselineService,
                new DailyChangeCalculator(),
                terminalServiceClient,
                botServiceClient
        );
    }

    @Test
    void getUserDashboard_calculatesDailyChangeFromBaselinePrices() {
        when(journalClient.getPositions("42")).thenReturn(List.of(
                new JournalPositionDto(42L, "BBG004730N88", BigDecimal.TEN, BigDecimal.valueOf(150))
        ));
        when(userServiceClient.getStocksByFigis(any())).thenReturn(List.of(
                new StockDto("BBG004730N88", "SBER", "Сбербанк", "RUB")
        ));
        when(priceServiceClient.getPrices(List.of("BBG004730N88"))).thenReturn(List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(110), null)
        ));
        when(priceBaselineService.resolveBaselinePrices(List.of("BBG004730N88"))).thenReturn(Map.of(
                "BBG004730N88", BigDecimal.valueOf(100)
        ));
        when(terminalServiceClient.getUserOrders("42")).thenReturn(List.of());
        when(botServiceClient.getUserBots("42")).thenReturn(List.of());

        DashboardDto dashboard = dashboardService.getUserDashboard("42");

        assertThat(dashboard.getDailyChange()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(dashboard.getDailyChangePercent()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(dashboard.getAssets()).singleElement().satisfies(asset -> {
            assertThat(asset.getTicker()).isEqualTo("SBER");
            assertThat(asset.getName()).isEqualTo("Сбербанк");
            assertThat(asset.getCurrency()).isEqualTo("RUB");
            assertThat(asset.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(110));
        });
    }

    @Test
    void getUserDashboard_whenJournalEmpty_fallsBackToUserServicePositions() {
        when(journalClient.getPositions("42")).thenReturn(List.of());
        when(userServiceClient.getUserPositions("42")).thenReturn(List.of(
                new PositionDto("BBG004730N88", "SBER", "Сбербанк", BigDecimal.ONE, "RUB")
        ));
        when(userServiceClient.getStocksByFigis(any())).thenReturn(List.of(
                new StockDto("BBG004730N88", "SBER", "Сбербанк", "RUB")
        ));
        when(priceServiceClient.getPrices(List.of("BBG004730N88"))).thenReturn(List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(200), null)
        ));
        when(priceBaselineService.resolveBaselinePrices(List.of("BBG004730N88"))).thenReturn(Map.of(
                "BBG004730N88", BigDecimal.valueOf(180)
        ));
        when(terminalServiceClient.getUserOrders("42")).thenReturn(List.of());
        when(botServiceClient.getUserBots("42")).thenReturn(List.of());

        DashboardDto dashboard = dashboardService.getUserDashboard("42");

        verify(userServiceClient).getUserPositions("42");
        assertThat(dashboard.getTotalPositions()).isEqualTo(1);
        assertThat(dashboard.getDailyChange()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void getUserDashboard_whenBaselineUnavailable_returnsZeroDailyChange() {
        when(journalClient.getPositions("42")).thenReturn(List.of(
                new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ONE, BigDecimal.valueOf(150))
        ));
        when(userServiceClient.getStocksByFigis(any())).thenReturn(List.of());
        when(priceServiceClient.getPrices(List.of("BBG004730N88"))).thenReturn(List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(200), null)
        ));
        when(priceBaselineService.resolveBaselinePrices(List.of("BBG004730N88"))).thenReturn(Map.of());
        when(terminalServiceClient.getUserOrders("42")).thenReturn(List.of());
        when(botServiceClient.getUserBots("42")).thenReturn(List.of());

        DashboardDto dashboard = dashboardService.getUserDashboard("42");

        assertThat(dashboard.getDailyChange()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dashboard.getDailyChangePercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
