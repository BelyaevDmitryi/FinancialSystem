package com.fs.adapter;

import com.fs.dto.AmendBrokerOrderDto;
import com.fs.mapper.StockPriceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.contract.v1.PriceType;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.OrdersService;
import ru.tinkoff.piapi.core.StopOrdersService;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TinkoffBrokerAdapterTest {

    private static final String ACCOUNT = "acc-live-1";
    private static final String ORDER_ID = "exchange-order-1";
    private static final String STOP_ORDER_ID = "stop-order-1";

    @Mock
    private InvestApi investApi;

    @Mock
    private OrdersService ordersService;

    @Mock
    private StopOrdersService stopOrdersService;

    private TinkoffBrokerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TinkoffBrokerAdapter(investApi, new StockPriceMapper());
    }

    @Test
    void amendOrder_limit_callsReplaceOrderAndReturnsUpdatedStatus() {
        OrderState current = limitOrderState(ORDER_ID, 2L, "250.50");
        OrderState amended = limitOrderState(ORDER_ID, 3L, "251.00");

        when(investApi.getOrdersService()).thenReturn(ordersService);
        when(ordersService.getOrderState(ACCOUNT, ORDER_ID))
                .thenReturn(CompletableFuture.completedFuture(current))
                .thenReturn(CompletableFuture.completedFuture(amended));
        when(ordersService.replaceOrder(
                eq(ORDER_ID),
                eq(3L),
                any(Quotation.class),
                eq(ACCOUNT),
                any(String.class),
                eq(PriceType.PRICE_TYPE_CURRENCY)))
                .thenReturn(CompletableFuture.completedFuture(PostOrderResponse.newBuilder()
                        .setOrderId(ORDER_ID)
                        .build()));

        var result = adapter.amendOrder(ACCOUNT, ORDER_ID, new AmendBrokerOrderDto(new BigDecimal("251.00"), 3L));

        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getOrderType()).isEqualTo("LIMIT");
        assertThat(result.getQuantity()).isEqualTo(3L);
        verify(ordersService).replaceOrder(
                eq(ORDER_ID),
                eq(3L),
                any(Quotation.class),
                eq(ACCOUNT),
                any(String.class),
                eq(PriceType.PRICE_TYPE_CURRENCY));
    }

    @Test
    void amendOrder_market_throwsIllegalState() {
        OrderState marketOrder = OrderState.newBuilder()
                .setOrderId(ORDER_ID)
                .setOrderType(OrderType.ORDER_TYPE_MARKET)
                .setExecutionReportStatus(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW)
                .build();

        when(investApi.getOrdersService()).thenReturn(ordersService);
        when(ordersService.getOrderState(ACCOUNT, ORDER_ID))
                .thenReturn(CompletableFuture.completedFuture(marketOrder));

        assertThatThrownBy(() -> adapter.amendOrder(
                ACCOUNT, ORDER_ID, new AmendBrokerOrderDto(new BigDecimal("100"), 1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LIMIT");
    }

    @Test
    void cancelOrder_stopOrder_usesStopOrdersService() {
        when(investApi.getStopOrdersService()).thenReturn(stopOrdersService);
        when(stopOrdersService.cancelStopOrder(ACCOUNT, STOP_ORDER_ID))
                .thenReturn(CompletableFuture.completedFuture(null));

        @SuppressWarnings("unchecked")
        var stopOrderIds = (java.util.Set<String>) ReflectionTestUtils.getField(adapter, "stopOrderIds");
        stopOrderIds.add(STOP_ORDER_ID);

        adapter.cancelOrder(ACCOUNT, STOP_ORDER_ID);

        verify(stopOrdersService).cancelStopOrder(ACCOUNT, STOP_ORDER_ID);
    }

    @Test
    void cancelOrder_exchangeOrder_usesOrdersService() {
        when(investApi.getOrdersService()).thenReturn(ordersService);
        when(ordersService.cancelOrder(ACCOUNT, ORDER_ID))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter.cancelOrder(ACCOUNT, ORDER_ID);

        verify(ordersService).cancelOrder(ACCOUNT, ORDER_ID);
    }

    private static OrderState limitOrderState(String orderId, long lots, String price) {
        String[] parts = price.split("\\.");
        long units = Long.parseLong(parts[0]);
        int nano = parts.length > 1
                ? new BigDecimal("0." + parts[1]).movePointRight(9).intValue()
                : 0;
        return OrderState.newBuilder()
                .setOrderId(orderId)
                .setFigi("BBG004730N88")
                .setLotsRequested(lots)
                .setLotsExecuted(0L)
                .setDirection(OrderDirection.ORDER_DIRECTION_BUY)
                .setOrderType(OrderType.ORDER_TYPE_LIMIT)
                .setExecutionReportStatus(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW)
                .setInitialSecurityPrice(MoneyValue.newBuilder()
                        .setUnits(units)
                        .setNano(nano)
                        .setCurrency("rub")
                        .build())
                .build();
    }
}
