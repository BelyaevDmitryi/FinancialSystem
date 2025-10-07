package com.fs.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    private static final LocalTime START_TIME = LocalTime.of(10, 0);
    private static final LocalTime END_TIME = LocalTime.of(18, 0);

    @Override
    public Health health() {
        LocalTime now = LocalTime.now();

        if (now.isAfter(START_TIME) && now.isBefore(END_TIME)) {
            return Health.up()
                    .withDetail("time", now.toString())
                    .withDetail("status", "Exchange is open!")
                    .build();
        } else {
            return Health.down()
                    .withDetail("time", now.toString())
                    .withDetail("status", "Exchange is outside operational hours!")
                    .build();
        }
    }
}
