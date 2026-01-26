package com.fs.controller;

import com.fs.dto.DashboardDto;
import com.fs.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard Controller", description = "API для дашборда с активами")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Получить дашборд пользователя")
    public ResponseEntity<DashboardDto> getUserDashboard(@org.springframework.web.bind.annotation.RequestHeader("X-User-Id") String userId) {
        log.info("Запрос дашборда для пользователя: {}", userId);
        DashboardDto dashboard = dashboardService.getUserDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }
}
