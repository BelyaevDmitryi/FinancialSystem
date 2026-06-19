package com.fs.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Генерация S2S JWT для интеграционных тестов ingest (ROLE_INTERNAL).
 * Секрет совпадает с {@code application-test.yml}.
 */
public final class InternalJwtTestSupport {

    private static final String SECRET =
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    private InternalJwtTestSupport() {
    }

    public static String internalToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3_600_000L);
        return Jwts.builder()
                .subject("trading-terminal-service")
                .claim("type", "access")
                .claim("roles", List.of("ROLE_INTERNAL"))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
