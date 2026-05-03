package com.fs.controller;

import com.fs.dto.JwtResponseDto;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.RefreshRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.domain.User;
import com.fs.repository.UserRepository;
import com.fs.security.UserDetailsServiceImpl;
import com.fs.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/signin")
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
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshRequestDto request) {
        JwtResponseDto dto = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequestDto signUpRequest) {
        authService.registerUser(signUpRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return ResponseEntity.ok(response);
    }
}

