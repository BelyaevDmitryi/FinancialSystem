package com.fs.service;

import com.fs.domain.BotStatus;
import com.fs.domain.OrderStatus;
import com.fs.dto.*;
import com.fs.feignclient.PriceServiceClient;
import com.fs.feignclient.TradingBotServiceClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.feignclient.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserServiceClient userServiceClient;
    private final PriceServiceClient priceServiceClient;
    private final TradingTerminalServiceClient terminalServiceClient;
    private final TradingBotServiceClient botServiceClient;

    public DashboardDto getUserDashboard(String userId) {
        log.info("Получение дашборда для пользователя: {}", userId);
        
        try {
            List<PositionDto> positions = userServiceClient.getUserPositions(userId);
            List<String> figies = positions.stream()
                    .map(PositionDto::getFigi)
                    .collect(Collectors.toList());

            List<PriceDataDto> prices = priceServiceClient.getPrices(figies);
            Map<String, BigDecimal> priceMap = prices.stream()
                    .collect(Collectors.toMap(PriceDataDto::getFigi, PriceDataDto::getPrice));

            List<AssetDto> assets = positions.stream()
                    .map(position -> {
                        BigDecimal currentPrice = priceMap.getOrDefault(position.getFigi(), BigDecimal.ZERO);
                        BigDecimal totalValue = currentPrice.multiply(position.getQuantity());
                        return new AssetDto(
                                position.getFigi(),
                                position.getTicker(),
                                position.getName(),
                                position.getQuantity(),
                                currentPrice,
                                totalValue,
                                position.getCurrency()
                        );
                    })
                    .collect(Collectors.toList());

            BigDecimal totalPortfolioValue = assets.stream()
                    .map(AssetDto::getTotalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<OrderDto> orders = terminalServiceClient.getUserOrders(userId);
            long pendingOrders = orders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.PENDING)
                    .count();

            List<BotDto> bots = botServiceClient.getUserBots(userId);
            long activeBots = bots.stream()
                    .filter(bot -> bot.getStatus() == BotStatus.ACTIVE)
                    .count();

            return new DashboardDto(
                    userId,
                    totalPortfolioValue,
                    assets.size(),
                    (int) activeBots,
                    (int) pendingOrders,
                    assets,
                    BigDecimal.ZERO, // TODO: рассчитать дневное изменение
                    BigDecimal.ZERO // TODO: рассчитать процент изменения
            );
        } catch (Exception e) {
            log.error("Ошибка при получении дашборда для пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Не удалось получить данные дашборда", e);
        }
    }
}
