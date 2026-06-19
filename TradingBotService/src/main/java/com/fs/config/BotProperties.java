package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private int candleLookback = 30;
    private String candleInterval = "DAY";
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Scheduler {
        private long fixedDelay = 60000;
    }
}
