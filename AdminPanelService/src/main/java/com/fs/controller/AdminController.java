package com.fs.controller;

import com.fs.dto.SystemStatsDto;
import com.fs.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Controller", description = "API для административной панели")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "Получить статистику системы")
    public ResponseEntity<?> getSystemStats(@RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
        // Проверяем наличие ролей ADMIN или OWNER
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            log.warn("Попытка доступа к админ панели без ролей");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Доступ запрещен");
        }

        List<String> roles = Arrays.asList(rolesHeader.split(","));
        boolean hasAdminAccess = roles.stream()
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_OWNER"));

        if (!hasAdminAccess) {
            log.warn("Попытка доступа к админ панели без необходимых прав. Роли: {}", roles);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Доступ запрещен. Требуются права администратора");
        }

        log.info("Запрос статистики системы");
        SystemStatsDto stats = adminService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
}
