package com.fs.support;

import com.fs.feignclient.AnalyticsServiceClient;
import com.fs.feignclient.PriceServiceClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestFeignConfig {

    @Bean
    public AnalyticsServiceClient analyticsServiceClient() {
        return mock(AnalyticsServiceClient.class);
    }

    @Bean
    public PriceServiceClient priceServiceClient() {
        return mock(PriceServiceClient.class);
    }

    @Bean
    public TradingTerminalServiceClient tradingTerminalServiceClient() {
        return mock(TradingTerminalServiceClient.class);
    }
}
