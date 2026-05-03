package com.fs.security.jwt;

import com.fs.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:900000}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String userId = String.valueOf(((UserDetailsServiceImpl.UserPrincipal) userDetails).getId());
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        if (userDetails.getAuthorities() != null) {
            claims.put("roles", userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList());
        }
        return createToken(claims, userId, expiration);
    }

    /** Генерация refresh-токена (длинный срок жизни, только для обмена на access). */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String userId = String.valueOf(((UserDetailsServiceImpl.UserPrincipal) userDetails).getId());
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        if (userDetails.getAuthorities() != null) {
            claims.put("roles", userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList());
        }
        return createToken(claims, userId, refreshExpiration);
    }
    
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof java.util.List) {
            return (java.util.List<String>) rolesObj;
        }
        return java.util.Collections.emptyList();
    }

    private String createToken(Map<String, Object> claims, String subject, long validityMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + validityMs))
                .signWith(getSigningKey())
                .compact();
    }

    /** Валидация refresh-токена: подпись и срок, тип claim = refresh. */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object type = claims.get(CLAIM_TYPE);
            return TYPE_REFRESH.equals(type) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getExpirationMs() {
        return expiration;
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

