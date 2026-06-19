package com.fs.support;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "docker.tests", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestFeignConfig.class)
class TradingBotSmokeTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
