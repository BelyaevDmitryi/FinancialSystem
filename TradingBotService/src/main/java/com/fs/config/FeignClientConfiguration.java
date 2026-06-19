package com.fs.config;

import com.fs.security.InternalServiceJwtProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Настройка Feign для TradingBotService:
 * - из HTTP-контекста (API-запрос) — пробрасывает Authorization пользователя;
 * - вне HTTP-контекста (scheduler) — добавляет S2S JWT в X-Gateway-Internal-Jwt.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignClientConfiguration {

    private static final String INTERNAL_JWT_HEADER = "X-Gateway-Internal-Jwt";

    private final InternalServiceJwtProvider internalServiceJwtProvider;

    @Bean
    public RequestInterceptor authorizationInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(auth)) {
                    template.header(HttpHeaders.AUTHORIZATION, auth);
                    return;
                }
            }
            // Вне HTTP-контекста (планировщик) — используем S2S JWT.
            String serviceToken = internalServiceJwtProvider.generateServiceToken();
            template.header(INTERNAL_JWT_HEADER, serviceToken);
            log.debug("Добавлен S2S JWT в заголовок {} для исходящего Feign-запроса", INTERNAL_JWT_HEADER);
        };
    }
}
