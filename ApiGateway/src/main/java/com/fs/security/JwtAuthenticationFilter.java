package com.fs.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * Сырой access JWT для внутренних вызовов: при проксировании через Gateway заголовок
     * {@code Authorization} до микросервисов иногда не доходит, а кастомный заголовок передаётся.
     * Клиентский запрос очищаем от подделки перед подстановкой проверенного токена.
     */
    public static final String GATEWAY_INTERNAL_JWT_HEADER = "X-Gateway-Internal-Jwt";

    @Autowired
    private JwtUtils jwtUtils;

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/signin",
            "/api/auth/signup",
            "/api/auth/refresh",
            "/api-docs",
            "/swagger-ui",
            "/actuator"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // CORS preflight не содержит Authorization — не требуем JWT, иначе браузер блокирует цепочку после логина.
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Пропускаем публичные пути
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = getTokenFromRequest(request);

        if (token == null) {
            log.warn("JWT токен отсутствует для пути: {}", path);
            return onError(exchange, "JWT токен отсутствует", HttpStatus.UNAUTHORIZED);
        }

        if (!jwtUtils.validateToken(token)) {
            log.warn("Невалидный JWT токен для пути: {}", path);
            return onError(exchange, "Невалидный JWT токен", HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = jwtUtils.extractUserId(token);
            List<String> roles = jwtUtils.extractRoles(token);
            
            // Добавляем userId, роли и внутренний JWT для микросервисов (см. GATEWAY_INTERNAL_JWT_HEADER).
            ServerHttpRequest.Builder requestBuilder = request.mutate();
            requestBuilder.headers(headers -> headers.remove(GATEWAY_INTERNAL_JWT_HEADER));
            requestBuilder.header("X-User-Id", userId);

            // Добавляем роли в заголовок как строку через запятую
            if (!roles.isEmpty()) {
                requestBuilder.header("X-User-Roles", String.join(",", roles));
            }

            // Повторно выставляем Authorization после проверки: до сервисов клиентский Bearer иногда теряется при прокси.
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            requestBuilder.header(GATEWAY_INTERNAL_JWT_HEADER, token);

            ServerHttpRequest modifiedRequest = requestBuilder.build();

            log.debug("JWT токен валидирован для пользователя с ID: {} и ролями: {} на пути: {}", userId, roles, path);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("Ошибка при обработке JWT токена: {}", e.getMessage());
            return onError(exchange, "Ошибка при обработке JWT токена", HttpStatus.UNAUTHORIZED);
        }
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // Высокий приоритет, чтобы выполняться раньше других фильтров
    }
}
