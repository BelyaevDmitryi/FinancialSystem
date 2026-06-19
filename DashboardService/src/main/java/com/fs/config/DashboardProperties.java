package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "dashboard")
public class DashboardProperties {

    private PriceBaseline priceBaseline = PriceBaseline.REDIS_SNAPSHOT;

    public enum PriceBaseline {
        MARKET_HISTORY_D1,
        REDIS_SNAPSHOT
    }
}
