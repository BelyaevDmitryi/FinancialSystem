package com.fs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BotProperties.class, MarketHistoryCandleProperties.class, FsBotProperties.class})
public class BotConfiguration {
}
