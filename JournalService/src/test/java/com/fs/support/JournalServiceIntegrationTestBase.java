package com.fs.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Базовый класс для интеграционных тестов JournalService.
 * PostgreSQL через Testcontainers (профиль {@code test}).
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class JournalServiceIntegrationTestBase {
}
