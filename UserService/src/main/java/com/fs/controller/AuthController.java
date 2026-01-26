package com.fs.controller;

import com.fs.dto.JwtResponseDto;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.domain.User;
import com.fs.repository.UserRepository;
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

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        String jwt = authService.authenticateUser(loginRequest);
        
        User user = userRepository.findByName(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new JwtResponseDto(
                jwt,
                "Bearer",
                String.valueOf(user.getId()),
                user.getName(), // username (логин)
                user.getNickname() != null ? user.getNickname() : user.getName(), // name (никнейм или логин по умолчанию)
                user.getRoles().stream().toList()
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequestDto signUpRequest) {
        authService.registerUser(signUpRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return ResponseEntity.ok(response);
    }
}

