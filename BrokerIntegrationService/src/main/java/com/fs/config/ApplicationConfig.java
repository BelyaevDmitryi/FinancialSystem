package com.fs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.tinkoff.piapi.core.InvestApi;

@Configuration
@EnableConfigurationProperties({ApiConfig.class, MoexConfig.class})
@RequiredArgsConstructor
@Profile("!test")
public class ApplicationConfig {
    private final ApiConfig apiConfig;

    @Bean
    public InvestApi InvestApi() {
        return InvestApi.create(apiConfig.getToken());
    }
}
