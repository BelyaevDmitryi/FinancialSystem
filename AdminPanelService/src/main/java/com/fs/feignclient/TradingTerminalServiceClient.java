package com.fs.feignclient;

import com.fs.dto.OrderStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "trading-terminal-service")
public interface TradingTerminalServiceClient {

    /**
     * Глобальная статистика ордеров (только для администратора).
     * Передавайте X-User-Id и X-User-Roles из запроса пользователя.
     */
    @GetMapping("/orders/admin/stats/orders")
    OrderStatsDto getOrderStats(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Roles") String rolesHeader);
}
