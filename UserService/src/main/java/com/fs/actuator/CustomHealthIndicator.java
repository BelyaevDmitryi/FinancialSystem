package com.fs.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Кастомный health indicator для проверки статуса биржи.
 * Всегда возвращает UP для сервиса, но добавляет информацию о статусе биржи в детали.
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

    private static final LocalTime START_TIME = LocalTime.of(10, 0);
    private static final LocalTime END_TIME = LocalTime.of(18, 0);

    @Override
    public Health health() {
        LocalTime now = LocalTime.now();
        boolean isExchangeOpen = now.isAfter(START_TIME) && now.isBefore(END_TIME);

        // Всегда возвращаем UP для сервиса, чтобы Eureka не помечал его как DOWN
        // Статус биржи передаем в деталях
        return Health.up()
                .withDetail("time", now.toString())
                .withDetail("exchangeOpen", isExchangeOpen)
                .withDetail("exchangeStatus", isExchangeOpen 
                        ? "Exchange is open!" 
                        : "Exchange is outside operational hours!")
                .build();
    }
}
