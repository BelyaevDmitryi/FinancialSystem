package com.fs.service;

import com.fs.dto.BotStatsDto;
import com.fs.dto.OrderStatsDto;
import com.fs.dto.SystemStatsDto;
import com.fs.feignclient.TradingBotServiceClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.feignclient.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserServiceClient userServiceClient;
    private final TradingTerminalServiceClient terminalServiceClient;
    private final TradingBotServiceClient botServiceClient;

    public SystemStatsDto getSystemStats() {
        log.info("Получение статистики системы");
        
        try {
            Long totalUsers = userServiceClient.getTotalUsers();
            OrderStatsDto orderStats = terminalServiceClient.getOrderStats();
            BotStatsDto botStats = botServiceClient.getBotStats();

            return new SystemStatsDto(
                    totalUsers != null ? totalUsers : 0L,
                    orderStats != null ? orderStats.getTotalOrders() : 0L,
                    botStats != null ? botStats.getActiveBots() : 0L,
                    orderStats != null ? orderStats.getOrdersByStatus() : new HashMap<>(),
                    botStats != null ? botStats.getBotsByStrategy() : new HashMap<>()
            );
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return new SystemStatsDto(0L, 0L, 0L, new HashMap<>(), new HashMap<>());
        }
    }
}
