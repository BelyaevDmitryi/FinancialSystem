package com.fs.factory;

import com.fs.adapter.BrokerAdapter;
import com.fs.dto.OrderBookDto;
import com.fs.dto.OrderBookEntryDto;
import com.fs.exception.StockNotFoundException;
import com.fs.model.Currency;
import com.fs.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrokerFactoryFallbackTest {

    @Mock
    private BrokerAdapter tinkoffAdapter;

    @Mock
    private BrokerAdapter moexAdapter;

    private BrokerFactory brokerFactory;

    @BeforeEach
    void setUp() {
        when(tinkoffAdapter.getBrokerName()).thenReturn("TINKOFF");
        when(moexAdapter.getBrokerName()).thenReturn("MOEX_ISS");
        when(tinkoffAdapter.isAvailable()).thenReturn(true);
        when(moexAdapter.isAvailable()).thenReturn(true);
        brokerFactory = new BrokerFactory(List.of(tinkoffAdapter, moexAdapter));
    }

    @Test
    void getStockByTickerWithFallback_tinkoffFound_returnsTinkoffStock() {
        Stock tinkoffStock = new Stock("SBER", "BBG004730N88", "Sberbank", "SHARE", Currency.RUB, "TINKOFF");
        when(tinkoffAdapter.getStockByTicker("SBER")).thenReturn(tinkoffStock);

        Stock result = brokerFactory.getStockByTickerWithFallback("SBER");

        assertThat(result).isEqualTo(tinkoffStock);
        verify(tinkoffAdapter).getStockByTicker("SBER");
    }

    @Test
    void getStockByTickerWithFallback_tinkoff404_returnsMoexStock() {
        Stock moexStock = new Stock("MOEXONLY", "MOEX:MOEXONLY", "Moex Only", "SHARE", Currency.RUB, "MOEX_ISS");
        when(tinkoffAdapter.getStockByTicker("MOEXONLY"))
                .thenThrow(new StockNotFoundException("Stock MOEXONLY not found in Tinkoff"));
        when(moexAdapter.getStockByTicker("MOEXONLY")).thenReturn(moexStock);

        Stock result = brokerFactory.getStockByTickerWithFallback("MOEXONLY");

        assertThat(result).isEqualTo(moexStock);
        verify(moexAdapter).getStockByTicker("MOEXONLY");
    }

    @Test
    void getStockByTickerWithFallback_bothMissing_throwsNotFound() {
        when(tinkoffAdapter.getStockByTicker("MISSING"))
                .thenThrow(new StockNotFoundException("Stock MISSING not found in Tinkoff"));
        when(moexAdapter.getStockByTicker("MISSING"))
                .thenThrow(new StockNotFoundException("Stock MISSING not found in MOEX ISS"));

        assertThatThrownBy(() -> brokerFactory.getStockByTickerWithFallback("MISSING"))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessageContaining("TINKOFF or MOEX ISS");
    }

    @Test
    void getOrderBookWithFallback_moexFigi_tinkoffHasInstrument_usesTinkoffOrderBook() {
        Stock tinkoffStock = new Stock("VTBR", "BBG004730ZJ9", "VTB", "SHARE", Currency.RUB, "TINKOFF");
        OrderBookDto tinkoffBook = new OrderBookDto(
                "BBG004730ZJ9",
                List.of(new OrderBookEntryDto(java.math.BigDecimal.TEN, 100L)),
                List.of(),
                java.math.BigDecimal.TEN,
                java.time.Instant.now(),
                20);
        when(tinkoffAdapter.getStockByTicker("VTBR")).thenReturn(tinkoffStock);
        when(tinkoffAdapter.getOrderBook("BBG004730ZJ9", 20)).thenReturn(tinkoffBook);

        OrderBookDto result = brokerFactory.getOrderBookWithFallback("MOEX:VTBR", 20);

        assertThat(result).isEqualTo(tinkoffBook);
        verify(moexAdapter, never()).getOrderBook("MOEX:VTBR", 20);
    }

    @Test
    void getOrderBookWithFallback_moexFigi_tinkoffMissing_usesMoexOrderBook() {
        OrderBookDto moexBook = new OrderBookDto(
                "MOEX:VTBR",
                List.of(),
                List.of(),
                java.math.BigDecimal.valueOf(78.81),
                java.time.Instant.now(),
                20);
        when(tinkoffAdapter.getStockByTicker("VTBR"))
                .thenThrow(new StockNotFoundException("Stock VTBR not found in Tinkoff"));
        when(moexAdapter.getOrderBook("MOEX:VTBR", 20)).thenReturn(moexBook);

        OrderBookDto result = brokerFactory.getOrderBookWithFallback("MOEX:VTBR", 20);

        assertThat(result).isEqualTo(moexBook);
        verify(moexAdapter).getOrderBook("MOEX:VTBR", 20);
    }
}
