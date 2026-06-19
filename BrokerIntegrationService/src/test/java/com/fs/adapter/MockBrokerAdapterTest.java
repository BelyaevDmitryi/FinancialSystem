package com.fs.adapter;

import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fs.dto.BrokerCandleDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockBrokerAdapterTest {

    private static final String FIGI = "BBG004730N88";
    private static final String ACCOUNT = "test-account";

    private MockBrokerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockBrokerAdapter();
        adapter.setMarketPrice(FIGI, BigDecimal.valueOf(100));
    }

    @Test
    void placeOrder_market_returnsFillImmediately() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 1L, null, "BUY", "MARKET", null, null);

        BrokerOrderDto result = adapter.placeOrder(ACCOUNT, request);

        assertThat(result.getOrderId()).startsWith("MOCK-");
        assertThat(result.getStatus()).isEqualTo("FILL");
        assertThat(result.getOrderType()).isEqualTo("MARKET");
        assertThat(result.getExecutedQuantity()).isEqualTo(1L);
    }

    @Test
    void placeOrder_limit_sell_staysNewUntilPriceTouches() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 2L, BigDecimal.valueOf(105), "SELL", "LIMIT", null, null);

        BrokerOrderDto placed = adapter.placeOrder(ACCOUNT, request);
        assertThat(placed.getStatus()).isEqualTo("NEW");

        BrokerOrderDto stillPending = adapter.getOrderStatus(ACCOUNT, placed.getOrderId());
        assertThat(stillPending.getStatus()).isEqualTo("NEW");

        adapter.setMarketPrice(FIGI, BigDecimal.valueOf(106));
        BrokerOrderDto filled = adapter.getOrderStatus(ACCOUNT, placed.getOrderId());
        assertThat(filled.getStatus()).isEqualTo("FILL");
    }

    @Test
    void placeOrder_stop_sell_triggersWhenPriceCrossesStop() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 1L, null, "SELL", "STOP", BigDecimal.valueOf(95), null);

        BrokerOrderDto placed = adapter.placeOrder(ACCOUNT, request);
        assertThat(placed.getStatus()).isEqualTo("NEW");
        assertThat(placed.getOrderType()).isEqualTo("STOP");

        adapter.setMarketPrice(FIGI, BigDecimal.valueOf(94));
        BrokerOrderDto filled = adapter.getOrderStatus(ACCOUNT, placed.getOrderId());
        assertThat(filled.getStatus()).isEqualTo("FILL");
    }

    @Test
    void placeOrder_limit_withoutPrice_throws() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 1L, null, "BUY", "LIMIT", null, null);

        assertThatThrownBy(() -> adapter.placeOrder(ACCOUNT, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("лимитной");
    }

    @Test
    void amendOrder_limit_updatesPriceWhileNew() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 2L, BigDecimal.valueOf(105), "SELL", "LIMIT", null, null);
        BrokerOrderDto placed = adapter.placeOrder(ACCOUNT, request);

        BrokerOrderDto amended = adapter.amendOrder(ACCOUNT, placed.getOrderId(),
                new AmendBrokerOrderDto(BigDecimal.valueOf(110), null));

        assertThat(amended.getPrice()).isEqualByComparingTo("110");
        assertThat(amended.getStatus()).isEqualTo("NEW");
        BrokerOrderDto status = adapter.getOrderStatus(ACCOUNT, placed.getOrderId());
        assertThat(status.getPrice()).isEqualByComparingTo("110");
    }

    @Test
    void cancelOrder_stopPending_cancelsOnBroker() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 1L, null, "SELL", "STOP", BigDecimal.valueOf(95), null);

        BrokerOrderDto placed = adapter.placeOrder(ACCOUNT, request);
        assertThat(placed.getStatus()).isEqualTo("NEW");

        adapter.cancelOrder(ACCOUNT, placed.getOrderId());

        BrokerOrderDto cancelled = adapter.getOrderStatus(ACCOUNT, placed.getOrderId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_limitPending_cancelsOnBroker() {
        CreateBrokerOrderDto request = new CreateBrokerOrderDto(
                FIGI, 2L, BigDecimal.valueOf(105), "SELL", "LIMIT", null, null);

        BrokerOrderDto placed = adapter.placeOrder(ACCOUNT, request);
        assertThat(placed.getStatus()).isEqualTo("NEW");

        adapter.cancelOrder(ACCOUNT, placed.getOrderId());

        BrokerOrderDto cancelled = adapter.getOrderStatus(ACCOUNT, placed.getOrderId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void getBrokerName_returnsMOCK() {
        assertThat(adapter.getBrokerName()).isEqualTo("MOCK");
    }

    @Test
    void isAvailable_returnsTrue() {
        assertThat(adapter.isAvailable()).isTrue();
    }

    @Test
    void getHistoricCandles_fiveDayRange_returnsOneCandlePerDay() {
        Instant from = Instant.parse("2026-01-01T12:00:00Z");
        Instant to = Instant.parse("2026-01-05T23:59:59Z");

        List<BrokerCandleDto> candles = adapter.getHistoricCandles(FIGI, from, to, "DAY");

        assertThat(candles).hasSize(5);
        assertThat(candles.get(0).getTime()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(candles.get(0).getOpen()).isEqualByComparingTo("98");
        assertThat(candles.get(0).getHigh()).isEqualByComparingTo("98.49");
        assertThat(candles.get(0).getLow()).isEqualByComparingTo("97.51");
        assertThat(candles.get(0).getVolume()).isEqualTo(1_000L);
        assertThat(candles.get(4).getTime()).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"));
        assertThat(candles.get(4).getClose()).isEqualByComparingTo("102");
    }

    @Test
    void getHistoricCandles_usesConfiguredMarketPrice() {
        adapter.setMarketPrice(FIGI, BigDecimal.valueOf(250));

        List<BrokerCandleDto> candles = adapter.getHistoricCandles(
                FIGI,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                "DAY");

        assertThat(candles).hasSize(2);
        assertThat(candles.get(0).getOpen()).isEqualByComparingTo("245");
    }
}
