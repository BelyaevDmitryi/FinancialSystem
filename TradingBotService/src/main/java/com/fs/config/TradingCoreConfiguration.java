package com.fs.config;

import com.fs.trading.core.EmaTrendStrategy;
import com.fs.trading.core.MacdCrossoverStrategy;
import com.fs.trading.core.SmaCrossoverStrategy;
import com.fs.trading.core.VolatilityBreakoutStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradingCoreConfiguration {

    @Bean
    public SmaCrossoverStrategy smaCrossoverStrategy() {
        return new SmaCrossoverStrategy();
    }

    @Bean
    public MacdCrossoverStrategy macdCrossoverStrategy() {
        return new MacdCrossoverStrategy();
    }

    @Bean
    public EmaTrendStrategy emaTrendStrategy() {
        return new EmaTrendStrategy();
    }

    @Bean
    public VolatilityBreakoutStrategy volatilityBreakoutStrategy() {
        return new VolatilityBreakoutStrategy();
    }
}
