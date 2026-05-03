package com.fs.controller;

import com.fs.domain.BotStatus;
import com.fs.dto.BotDto;
import com.fs.dto.CreateBotDto;
import com.fs.service.TradingBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trading Bot Controller", description = "API для управления торговыми ботами")
public class TradingBotController {

    private final TradingBotService botService;

    @PostMapping
    @Operation(summary = "Создать нового торгового бота")
    public ResponseEntity<BotDto> createBot(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateBotDto createBotDto) {
        log.info("Создание бота для пользователя: {}", userId);
        BotDto bot = botService.createBot(userId, createBotDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(bot);
    }

    @GetMapping
    @Operation(summary = "Получить все боты пользователя")
    public ResponseEntity<List<BotDto>> getUserBots(@RequestHeader("X-User-Id") String userId) {
        log.info("Получение ботов для пользователя: {}", userId);
        List<BotDto> bots = botService.getUserBots(userId);
        return ResponseEntity.ok(bots);
    }

    @PutMapping("/{botId}/status")
    @Operation(summary = "Изменить статус бота")
    public ResponseEntity<BotDto> updateBotStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String botId,
            @RequestParam BotStatus status) {
        log.info("Изменение статуса бота {} на {}", botId, status);
        BotDto bot = botService.updateBotStatus(userId, botId, status);
        return ResponseEntity.ok(bot);
    }

    @DeleteMapping("/{botId}")
    @Operation(summary = "Удалить бота")
    public ResponseEntity<Void> deleteBot(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String botId) {
        log.info("Удаление бота {} пользователем {}", botId, userId);
        botService.deleteBot(userId, botId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/stats/bots")
    @Operation(summary = "Получить статистику ботов (для админ-панели)")
    public ResponseEntity<com.fs.dto.BotStatsDto> getBotStats() {
        log.info("Получение статистики ботов");
        return ResponseEntity.ok(botService.getBotStats());
    }
}
