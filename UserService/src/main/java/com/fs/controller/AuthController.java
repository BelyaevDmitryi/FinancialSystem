package com.fs.controller;

import com.fs.dto.JwtResponseDto;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.RefreshRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.domain.User;
import com.fs.repository.UserRepository;
import com.fs.security.UserDetailsServiceImpl;
import com.fs.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Регистрация, вход и обновление JWT (access + refresh)")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/signin")
    @Operation(
            summary = "Вход в систему",
            description = "Аутентификация по username и password. Возвращает access JWT, refresh JWT и профиль пользователя."
    )
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        String accessToken = authService.authenticateUser(loginRequest);
        User user = userRepository.findByName(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserDetailsServiceImpl.UserPrincipal principal = (UserDetailsServiceImpl.UserPrincipal)
                userDetailsService.loadUserById(user.getId());
        String refreshToken = authService.generateRefreshToken(principal);

        return ResponseEntity.ok(new JwtResponseDto(
                accessToken,
                refreshToken,
                authService.getAccessTokenExpirationMs(),
                String.valueOf(user.getId()),
                user.getName(),
                user.getNickname() != null ? user.getNickname() : user.getName(),
                user.getRoles().stream().toList()
        ));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновление JWT",
            description = "Обмен валидного refresh JWT на новую пару access + refresh без повторного ввода пароля. "
                    + "Ответ содержит только токены (без профиля пользователя)."
    )
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshRequestDto request) {
        JwtResponseDto dto = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/signup")
    @Operation(
            summary = "Регистрация пользователя",
            description = "Создание нового пользователя с ролью ROLE_USER. После регистрации используйте signin для получения токенов."
    )
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequestDto signUpRequest) {
        authService.registerUser(signUpRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return ResponseEntity.ok(response);
    }
}

