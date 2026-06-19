package com.fs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Генерирует S2S JWT для исходящих вызовов JournalService.
 */
@Component
@Slf4j
public class InternalServiceJwtProvider {

    private static final long TOKEN_VALIDITY_MS = 3_600_000L;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateServiceToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TOKEN_VALIDITY_MS);

        String token = Jwts.builder()
                .subject("trading-terminal-service")
                .claim("type", "access")
                .claim("roles", List.of("ROLE_INTERNAL"))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();

        log.debug("Сгенерирован S2S JWT для исходящего запроса к JournalService");
        return token;
    }
}
