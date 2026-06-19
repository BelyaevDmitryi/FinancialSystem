package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "market-history.candles")
public class MarketHistoryCandleProperties {

    private boolean enabled = false;
}
