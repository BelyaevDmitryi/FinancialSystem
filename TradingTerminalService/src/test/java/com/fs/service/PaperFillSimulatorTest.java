package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.PriceServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperFillSimulatorTest {

    @Mock
    private PriceServiceClient priceServiceClient;

    @InjectMocks
    private PaperFillSimulator paperFillSimulator;

    @Test
    void applyPaperFill_usesLastPriceFromPriceService() {
        when(priceServiceClient.getPrices(List.of("BBG004730N88")))
                .thenReturn(List.of(
                        new PriceDataDto("BBG004730N88", BigDecimal.valueOf(100), LocalDateTime.now()),
                        new PriceDataDto("BBG004730N88", BigDecimal.valueOf(123.45), LocalDateTime.now())
                ));

        Order order = new Order();
        order.setId(1L);
        order.setFigi("BBG004730N88");
        order.setType(OrderType.BUY);
        order.setPrice(BigDecimal.valueOf(99));
        order.setStatus(OrderStatus.PENDING);

        paperFillSimulator.applyPaperFill(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(order.getPrice()).isEqualByComparingTo("123.45");
        assertThat(order.isPaper()).isTrue();
        assertThat(order.getExecutedAt()).isNotNull();
    }

    @Test
    void resolveFillPrice_fallsBackWhenPriceServiceEmpty() {
        when(priceServiceClient.getPrices(List.of("BBG004730N88"))).thenReturn(List.of());

        BigDecimal price = paperFillSimulator.resolveFillPrice("BBG004730N88", BigDecimal.valueOf(88));

        assertThat(price).isEqualByComparingTo("88");
    }
}
