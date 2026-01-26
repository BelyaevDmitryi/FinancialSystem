package com.fs.controller;

import com.fs.domain.OrderStatus;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.service.OrderService;
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
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Controller", description = "API для управления торговыми ордерами")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Создать новый ордер")
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderDto createOrderDto) {
        log.info("Создание ордера для пользователя: {}", userId);
        OrderDto order = orderService.createOrder(userId, createOrderDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    @Operation(summary = "Получить все ордера пользователя")
    public ResponseEntity<List<OrderDto>> getUserOrders(@RequestHeader("X-User-Id") String userId) {
        log.info("Получение ордеров для пользователя: {}", userId);
        List<OrderDto> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Получить ордера пользователя по статусу")
    public ResponseEntity<List<OrderDto>> getUserOrdersByStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable OrderStatus status) {
        log.info("Получение ордеров для пользователя: {} со статусом: {}", userId, status);
        List<OrderDto> orders = orderService.getUserOrdersByStatus(userId, status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Получить ордер по ID")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String orderId) {
        log.info("Получение ордера: {}", orderId);
        OrderDto order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/execute")
    @Operation(summary = "Исполнить ордер")
    public ResponseEntity<OrderDto> executeOrder(@PathVariable String orderId) {
        log.info("Исполнение ордера: {}", orderId);
        OrderDto order = orderService.executeOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Отменить ордер")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable String orderId) {
        log.info("Отмена ордера: {}", orderId);
        OrderDto order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/admin/stats/orders")
    @Operation(summary = "Получить статистику ордеров (для админ-панели)")
    public ResponseEntity<com.fs.dto.OrderStatsDto> getOrderStats() {
        log.info("Получение статистики ордеров");
        return ResponseEntity.ok(orderService.getOrderStats());
    }
}
