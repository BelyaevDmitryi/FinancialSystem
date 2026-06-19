package com.fs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.RefreshRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.repository.UserRepository;
import com.fs.security.jwt.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String registeredUsername;

    @AfterEach
    void cleanup() {
        if (registeredUsername != null) {
            userRepository.findByName(registeredUsername)
                    .ifPresent(user -> userRepository.deleteById(user.getId()));
            registeredUsername = null;
        }
    }

    @Test
    void testSignupAndSignin() throws Exception {
        String username = uniqueUsername();
        registerUser(username, "Test User", "password123");

        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        var user = userRepository.findByName(username).orElseThrow();

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInMs").value(jwtUtils.getExpirationMs()))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(String.valueOf(user.getId())))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.name").value(username));
    }

    @Test
    void testSignupWithExistingUsername() throws Exception {
        String username = uniqueUsername();
        registerUser(username, "Existing User", "password123");

        SignupRequestDto duplicate = new SignupRequestDto();
        duplicate.setUsername(username);
        duplicate.setName("Existing User");
        duplicate.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error: Username is already taken!"));
    }

    @Test
    void testSigninWithInvalidCredentials() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername("nonexistent-" + UUID.randomUUID());
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTokenEndpoint {

        @Test
        void refreshWithValidTokenReturnsNewTokenPair() throws Exception {
            TokenPair tokens = signUpAndSignIn();

            MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequestDto(tokens.refreshToken()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.expiresInMs").value(jwtUtils.getExpirationMs()))
                    .andReturn();

            JsonNode body = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
            String newAccess = body.get("token").asText();
            String newRefresh = body.get("refreshToken").asText();

            assertNotNull(newAccess);
            assertNotNull(newRefresh);
            assertFalse(jwtUtils.validateRefreshToken(newAccess));
            assertTrue(jwtUtils.validateRefreshToken(newRefresh));
        }

        @Test
        void emptyRefreshTokenReturns400() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void garbageRefreshTokenReturns401() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequestDto("not-a-valid-jwt"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        void accessTokenInsteadOfRefreshReturns401() throws Exception {
            TokenPair tokens = signUpAndSignIn();

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequestDto(tokens.accessToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        void expiredRefreshTokenReturns401() throws Exception {
            signUpAndSignIn();
            String expiredRefresh = buildExpiredRefreshToken(
                    userRepository.findByName(registeredUsername).orElseThrow().getId());

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RefreshRequestDto(expiredRefresh))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    private TokenPair signUpAndSignIn() throws Exception {
        String username = uniqueUsername();
        registerUser(username, "Refresh User", "password123");

        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        MvcResult signinResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresInMs").value(jwtUtils.getExpirationMs()))
                .andReturn();

        JsonNode body = objectMapper.readTree(signinResult.getResponse().getContentAsString());
        return new TokenPair(body.get("token").asText(), body.get("refreshToken").asText());
    }

    private void registerUser(String username, String displayName, String password) throws Exception {
        SignupRequestDto signupRequest = new SignupRequestDto();
        signupRequest.setUsername(username);
        signupRequest.setName(displayName);
        signupRequest.setPassword(password);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        registeredUsername = username;
    }

    private String uniqueUsername() {
        return "auth-test-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String buildExpiredRefreshToken(Long userId) {
        var signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(Map.of("type", "refresh"))
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now - 10_000))
                .expiration(new Date(now - 5_000))
                .signWith(signingKey)
                .compact();
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
