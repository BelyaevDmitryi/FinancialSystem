package com.fs.service;

import com.fs.adapter.BrokerAdapter;
import com.fs.factory.BrokerFactory;
import com.fs.model.Currency;
import com.fs.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrokerIntegrationServiceStockTest {

    @Mock
    private BrokerFactory brokerFactory;

    @Mock
    private BrokerAdapter mockAdapter;

    private BrokerIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new BrokerIntegrationService(brokerFactory);
    }

    @Test
    void getStockByTicker_mockDefault_usesMockAdapterNotTinkoffFallback() {
        ReflectionTestUtils.setField(service, "defaultBroker", "MOCK");
        Stock mockStock = new Stock("SBER", "MOCK-FIGI-SBER", "MOCK SBER", "STOCK",
                Currency.RUB, "MOCK");
        when(brokerFactory.getBrokerAdapter("MOCK")).thenReturn(mockAdapter);
        when(mockAdapter.getStockByTicker("SBER")).thenReturn(mockStock);

        Stock result = service.getStockByTicker("SBER");

        assertThat(result.getFigi()).isEqualTo("MOCK-FIGI-SBER");
        verify(brokerFactory).getBrokerAdapter("MOCK");
        verify(brokerFactory, never()).getStockByTickerWithFallback("SBER");
        verify(mockAdapter).getStockByTicker("SBER");
    }

    @Test
    void getStockByTicker_tinkoffDefault_usesFallbackChain() {
        ReflectionTestUtils.setField(service, "defaultBroker", "TINKOFF");
        Stock tinkoffStock = new Stock("SBER", "BBG004730N88", "Sberbank", "STOCK",
                Currency.RUB, "TINKOFF");
        when(brokerFactory.getStockByTickerWithFallback("SBER")).thenReturn(tinkoffStock);

        Stock result = service.getStockByTicker("SBER");

        assertThat(result.getFigi()).isEqualTo("BBG004730N88");
        verify(brokerFactory).getStockByTickerWithFallback("SBER");
    }
}
