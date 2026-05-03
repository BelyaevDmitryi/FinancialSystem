package com.fs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class JwtResponseDto {
    private String token;
    private String type = "Bearer";
    private String refreshToken;
    private Long expiresInMs;
    private String id;
    private String username;
    private String name;
    private List<String> roles;

    /** Ответ при логине: access + refresh токены и данные пользователя. */
    public JwtResponseDto(String token, String refreshToken, Long expiresInMs,
                          String id, String username, String name, List<String> roles) {
        this.token = token;
        this.type = "Bearer";
        this.refreshToken = refreshToken;
        this.expiresInMs = expiresInMs;
        this.id = id;
        this.username = username;
        this.name = name;
        this.roles = roles != null ? roles : List.of();
    }

    /** Ответ при refresh: только новые токены (без данных пользователя — фронт сохраняет их). */
    public static JwtResponseDto tokensOnly(String token, String refreshToken, Long expiresInMs) {
        JwtResponseDto dto = new JwtResponseDto();
        dto.setToken(token);
        dto.setType("Bearer");
        dto.setRefreshToken(refreshToken);
        dto.setExpiresInMs(expiresInMs);
        return dto;
    }
}

