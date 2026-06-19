package com.fs.config;

import com.fs.security.InternalServiceJwtProvider;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Feign-конфигурация для S2S-вызовов JournalService.
 */
public class JournalFeignClientConfiguration {

    private static final String INTERNAL_JWT_HEADER = "X-Gateway-Internal-Jwt";

    @Bean
    public RequestInterceptor journalServiceAuthInterceptor(InternalServiceJwtProvider internalServiceJwtProvider) {
        return template -> template.header(
                INTERNAL_JWT_HEADER,
                internalServiceJwtProvider.generateServiceToken()
        );
    }
}
