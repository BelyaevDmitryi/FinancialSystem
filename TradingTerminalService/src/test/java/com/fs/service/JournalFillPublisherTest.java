package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.dto.FillDto;
import com.fs.dto.TradeDto;
import com.fs.dto.TradeSide;
import com.fs.feignclient.JournalServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalFillPublisherTest {

    @Mock
    private JournalServiceClient journalServiceClient;

    @InjectMocks
    private JournalFillPublisher journalFillPublisher;

    private Order executedOrder;

    @BeforeEach
    void setUp() {
        executedOrder = new Order();
        executedOrder.setId(1001L);
        executedOrder.setUserId(42L);
        executedOrder.setFigi("BBG004730N88");
        executedOrder.setType(OrderType.BUY);
        executedOrder.setQuantity(BigDecimal.TEN);
        executedOrder.setPrice(BigDecimal.valueOf(150));
        executedOrder.setStatus(OrderStatus.EXECUTED);
        executedOrder.setExecutedAt(LocalDateTime.of(2026, 5, 24, 12, 0));
    }

    @Test
    void publishFill_postsFillDtoToJournal() {
        when(journalServiceClient.recordFill(eq(42L), any(FillDto.class)))
                .thenReturn(new TradeDto(1L, 42L, "BBG004730N88", TradeSide.BUY,
                        BigDecimal.TEN, BigDecimal.valueOf(150), null, 1001L, null,
                        executedOrder.getExecutedAt()));

        journalFillPublisher.publishFill(executedOrder);

        ArgumentCaptor<FillDto> fillCaptor = ArgumentCaptor.forClass(FillDto.class);
        verify(journalServiceClient).recordFill(eq(42L), fillCaptor.capture());
        FillDto fill = fillCaptor.getValue();
        assertThat(fill.orderId()).isEqualTo(1001L);
        assertThat(fill.figi()).isEqualTo("BBG004730N88");
        assertThat(fill.side()).isEqualTo(TradeSide.BUY);
        assertThat(fill.quantity()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(fill.price()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void publishFill_duplicateOrderId_idempotentOnJournalSide() {
        when(journalServiceClient.recordFill(eq(42L), any(FillDto.class)))
                .thenReturn(new TradeDto(1L, 42L, "BBG004730N88", TradeSide.BUY,
                        BigDecimal.TEN, BigDecimal.valueOf(150), null, 1001L, null,
                        executedOrder.getExecutedAt()));

        assertThatCode(() -> {
            journalFillPublisher.publishFill(executedOrder);
            journalFillPublisher.publishFill(executedOrder);
        }).doesNotThrowAnyException();

        verify(journalServiceClient, times(2)).recordFill(eq(42L), any(FillDto.class));
    }
}
