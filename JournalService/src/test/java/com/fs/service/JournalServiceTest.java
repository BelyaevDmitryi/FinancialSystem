package com.fs.service;

import com.fs.domain.Position;
import com.fs.domain.Trade;
import com.fs.domain.TradeSide;
import com.fs.dto.FillDto;
import com.fs.dto.TradeDto;
import com.fs.repository.PositionRepository;
import com.fs.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    private static final String FIGI = "BBG004730N88";
    private static final Long USER_ID = 42L;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private JournalService journalService;

    private final AtomicReference<Position> storedPosition = new AtomicReference<>();
    private final AtomicReference<Trade> storedTrade = new AtomicReference<>();

    @BeforeEach
    void stubRepositories() {
        when(positionRepository.findByUserIdAndFigi(USER_ID, FIGI))
                .thenAnswer(invocation -> Optional.ofNullable(storedPosition.get()));
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
            Position position = invocation.getArgument(0);
            storedPosition.set(position);
            return position;
        });
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            trade.setId(1L);
            storedTrade.set(trade);
            return trade;
        });
    }

    @Nested
    class BuyFill {

        @Test
        void buyFill_increasesPositionQuantity() {
            when(tradeRepository.findByOrderId(1001L)).thenReturn(Optional.empty());

            journalService.recordFill(fill(1001L, TradeSide.BUY, BigDecimal.TEN, BigDecimal.valueOf(150)));

            Position position = storedPosition.get();
            assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(position.getAvgPrice()).isEqualByComparingTo(BigDecimal.valueOf(150));
        }

        @Test
        void secondBuyFill_updatesWeightedAveragePrice() {
            when(tradeRepository.findByOrderId(1001L)).thenReturn(Optional.empty());
            when(tradeRepository.findByOrderId(1002L)).thenReturn(Optional.empty());

            journalService.recordFill(fill(1001L, TradeSide.BUY, BigDecimal.TEN, BigDecimal.valueOf(100)));
            journalService.recordFill(fill(1002L, TradeSide.BUY, BigDecimal.TEN, BigDecimal.valueOf(200)));

            Position position = storedPosition.get();
            assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(position.getAvgPrice()).isEqualByComparingTo(BigDecimal.valueOf(150));
        }
    }

    @Nested
    class SellFill {

        @Test
        void sellFill_calculatesRealizedPnl() {
            when(tradeRepository.findByOrderId(2001L)).thenReturn(Optional.empty());
            when(tradeRepository.findByOrderId(2002L)).thenReturn(Optional.empty());

            journalService.recordFill(fill(2001L, TradeSide.BUY, BigDecimal.TEN, BigDecimal.valueOf(100)));
            TradeDto sellTrade = journalService.recordFill(
                    fill(2002L, TradeSide.SELL, BigDecimal.valueOf(4), BigDecimal.valueOf(120)));

            assertThat(sellTrade.realizedPnl()).isEqualByComparingTo(BigDecimal.valueOf(80));

            Position position = storedPosition.get();
            assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(6));
            assertThat(position.getAvgPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    @Nested
    class Idempotency {

        @Test
        void duplicateOrderId_doesNotDoubleCount() {
            when(tradeRepository.findByOrderId(3001L))
                    .thenReturn(Optional.empty())
                    .thenAnswer(invocation -> Optional.of(storedTrade.get()));

            FillDto fill = fill(3001L, TradeSide.BUY, BigDecimal.TEN, BigDecimal.valueOf(150));
            TradeDto first = journalService.recordFill(fill);
            TradeDto second = journalService.recordFill(fill);

            assertThat(second.id()).isEqualTo(first.id());
            verify(tradeRepository, times(1)).save(any(Trade.class));
            verify(positionRepository, times(1)).save(any(Position.class));

            ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
            verify(positionRepository).save(positionCaptor.capture());
            assertThat(positionCaptor.getValue().getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
        }
    }

    private static FillDto fill(Long orderId, TradeSide side, BigDecimal quantity, BigDecimal price) {
        return new FillDto(
                USER_ID,
                orderId,
                FIGI,
                side,
                quantity,
                price,
                null,
                LocalDateTime.now()
        );
    }
}
