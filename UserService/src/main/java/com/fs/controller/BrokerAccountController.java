package com.fs.controller;

import com.fs.dto.*;
import com.fs.service.BrokerAccountService;
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
@RequestMapping("/api/profile/broker-accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Broker Accounts", description = "Привязка счетов пользователя у брокеров")
public class BrokerAccountController {

    private final BrokerAccountService brokerAccountService;

    @GetMapping("/brokers")
    @Operation(summary = "Список доступных брокеров")
    public ResponseEntity<List<BrokerDto>> getBrokers() {
        return ResponseEntity.ok(brokerAccountService.getAllBrokers());
    }

    @GetMapping
    @Operation(summary = "Мои счета у брокеров")
    public ResponseEntity<List<UserBrokerAccountDto>> getMyAccounts(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(brokerAccountService.getUserBrokerAccounts(userId));
    }

    @GetMapping("/default")
    @Operation(summary = "Счёт по умолчанию для брокера (для торговли)")
    public ResponseEntity<DefaultBrokerAccountDto> getDefaultAccount(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String broker) {
        DefaultBrokerAccountDto dto = brokerAccountService.getDefaultAccountForUserAndBroker(userId, broker);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(summary = "Привязать счёт у брокера")
    public ResponseEntity<UserBrokerAccountDto> addAccount(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateUserBrokerAccountDto dto) {
        UserBrokerAccountDto created = brokerAccountService.addAccount(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить счёт (название, счёт по умолчанию)")
    public ResponseEntity<UserBrokerAccountDto> updateAccount(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id,
            @RequestBody UpdateUserBrokerAccountDto dto) {
        return ResponseEntity.ok(brokerAccountService.updateAccount(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Отвязать счёт")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id) {
        brokerAccountService.deleteAccount(userId, id);
        return ResponseEntity.noContent().build();
    }
}
