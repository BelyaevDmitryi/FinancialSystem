package com.fs.controller;

import com.fs.dto.ChangePasswordDto;
import com.fs.dto.UpdateProfileDto;
import com.fs.dto.UserProfileDto;
import com.fs.exception.UserNotFoundException;
import com.fs.repository.UserRepository;
import com.fs.service.FileStorageService;
import com.fs.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(@RequestHeader("X-User-Id") String userId) {
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileDto updateProfileDto) {
        UserProfileDto updatedProfile = userService.updateUserProfile(userId, updateProfileDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        
        try {
            var user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Проверяем текущий пароль
            if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Неверный текущий пароль");
                log.warn("Неверный текущий пароль для пользователя {}", userId);
                return ResponseEntity.badRequest().body(error);
            }

            // Устанавливаем новый пароль
            user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Пароль успешно изменен");
            log.info("Пароль успешно изменен для пользователя {}", userId);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Неверный формат ID пользователя");
            log.error("Ошибка при парсинге ID пользователя: {}", userId, e);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ошибка при изменении пароля: " + e.getMessage());
            log.error("Ошибка при изменении пароля для пользователя {}", userId, e);
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Сохраняем файл
            String avatarPath = fileStorageService.saveAvatar(file);
            
            // Обновляем профиль пользователя
            UserProfileDto updatedProfile = userService.updateUserAvatar(userId, avatarPath);
            
            log.info("Аватар успешно загружен для пользователя {}", userId);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            log.warn("Ошибка валидации файла для пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ошибка при загрузке аватара: " + e.getMessage());
            log.error("Ошибка при загрузке аватара для пользователя {}", userId, e);
            return ResponseEntity.status(500).body(error);
        }
    }
}
