package com.fs.service;

import com.fs.domain.FigiWithPrice;
import com.fs.dto.FigiesDto;
import com.fs.dto.Stock;
import com.fs.dto.StockPrice;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.StocksWithPrices;
import com.fs.feignclient.StockServiceClient;
import com.fs.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockPriceServiceMoexFallbackTest {

    private static final String TINKOFF_FIGI = "BBG004731032";
    private static final String TICKER = "MOEXONLY";

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockServiceClient stockServiceClient;

    @InjectMocks
    private StockPriceService stockPriceService;

    @Test
    void getPrices_tinkoffEmpty_retriesMoexByTicker() {
        Stock stock = new Stock(TICKER, TINKOFF_FIGI, "Moex Only", "SHARE",
                com.fs.dto.Currency.RUB, "TINKOFF", null);
        StocksDto request = new StocksDto(List.of(stock));

        when(stockRepository.findAllById(List.of(TINKOFF_FIGI))).thenReturn(Collections.emptyList());
        when(stockServiceClient.getPrices(any(FigiesDto.class)))
                .thenReturn(new StocksPricesDto(Collections.emptyList()));
        when(stockServiceClient.getPrices(any(FigiesDto.class), eq("MOEX_ISS")))
                .thenReturn(new StocksPricesDto(List.of(
                        new StockPrice("MOEX:" + TICKER, BigDecimal.valueOf(123.45)))));

        StocksWithPrices result = stockPriceService.getPrices(request);

        assertThat(result.getStocks()).hasSize(1);
        assertThat(result.getStocks().get(0).getFigi()).isEqualTo(TINKOFF_FIGI);
        assertThat(result.getStocks().get(0).getPrice()).isEqualByComparingTo("123.45");
        assertThat(result.getStocks().get(0).getSource()).isEqualTo("MOEX_ISS");
        verify(stockServiceClient).getPrices(any(FigiesDto.class), eq("MOEX_ISS"));
    }
}
