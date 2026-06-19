package com.fs.service;

import com.fs.dto.JwtResponseDto;
import com.fs.exception.InvalidRefreshTokenException;
import com.fs.security.UserDetailsServiceImpl;
import com.fs.security.jwt.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String VALID_REFRESH = "valid-refresh-jwt";
    private static final long EXPIRATION_MS = 900_000L;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("refreshTokens")
    class RefreshTokens {

        @Test
        void validRefreshReturnsNewTokenPair() {
            UserDetailsServiceImpl.UserPrincipal principal = new UserDetailsServiceImpl.UserPrincipal(
                    42L,
                    "test-user",
                    "encoded-password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            when(jwtUtils.validateRefreshToken(VALID_REFRESH)).thenReturn(true);
            when(jwtUtils.extractUserId(VALID_REFRESH)).thenReturn("42");
            when(userDetailsService.loadUserById(42L)).thenReturn(principal);
            when(jwtUtils.generateToken(principal)).thenReturn("new-access");
            when(jwtUtils.generateRefreshToken(principal)).thenReturn("new-refresh");
            when(jwtUtils.getExpirationMs()).thenReturn(EXPIRATION_MS);

            JwtResponseDto result = authService.refreshTokens(VALID_REFRESH);

            assertNotNull(result);
            assertEquals("new-access", result.getToken());
            assertEquals("new-refresh", result.getRefreshToken());
            assertEquals("Bearer", result.getType());
            assertEquals(EXPIRATION_MS, result.getExpiresInMs());

            verify(jwtUtils).validateRefreshToken(VALID_REFRESH);
            verify(jwtUtils).extractUserId(VALID_REFRESH);
            verify(userDetailsService).loadUserById(42L);
            verify(jwtUtils).generateToken(principal);
            verify(jwtUtils).generateRefreshToken(principal);
        }

        @Test
        void invalidOrExpiredRefreshThrowsInvalidRefreshTokenException() {
            when(jwtUtils.validateRefreshToken("expired-or-invalid")).thenReturn(false);

            assertThrows(InvalidRefreshTokenException.class,
                    () -> authService.refreshTokens("expired-or-invalid"));

            verify(jwtUtils).validateRefreshToken("expired-or-invalid");
            verify(jwtUtils, never()).extractUserId(anyString());
            verify(userDetailsService, never()).loadUserById(anyLong());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void nullOrBlankRefreshThrowsInvalidRefreshTokenException(String refreshToken) {
            if (refreshToken != null) {
                when(jwtUtils.validateRefreshToken(refreshToken)).thenReturn(false);
            }

            assertThrows(InvalidRefreshTokenException.class,
                    () -> authService.refreshTokens(refreshToken));

            if (refreshToken == null) {
                verify(jwtUtils, never()).validateRefreshToken(anyString());
            } else {
                verify(jwtUtils).validateRefreshToken(refreshToken);
            }
        }
    }
}
