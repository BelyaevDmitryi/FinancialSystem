package com.fs.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Базовый класс для интеграционных тестов TradingTerminalService.
 * Поднимает полный Spring-контекст с Testcontainers PostgreSQL через профиль test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestFeignConfig.class)
public abstract class TradingTerminalIntegrationTestBase {
}
