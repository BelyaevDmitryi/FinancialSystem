package com.fs.support;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "docker.tests", matches = "true")
class JournalServiceSmokeTest extends JournalServiceIntegrationTestBase {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
