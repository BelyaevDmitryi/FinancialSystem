package com.fs.service;

import com.fs.domain.BotStatus;
import com.fs.domain.OrderStatus;
import com.fs.dto.*;
import com.fs.feignclient.JournalClient;
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

    private final JournalClient journalClient;
    private final UserServiceClient userServiceClient;
    private final PriceServiceClient priceServiceClient;
    private final PriceBaselineService priceBaselineService;
    private final DailyChangeCalculator dailyChangeCalculator;
    private final TradingTerminalServiceClient terminalServiceClient;
    private final TradingBotServiceClient botServiceClient;

    public DashboardDto getUserDashboard(String userId) {
        log.info("Получение дашборда для пользователя: {}", userId);

        try {
            List<PositionSnapshot> positions = resolvePositions(userId);
            List<String> figies = positions.stream()
                    .map(PositionSnapshot::figi)
                    .distinct()
                    .toList();

            Map<String, StockDto> stockMetadata = loadStockMetadata(figies);

            Map<String, BigDecimal> currentPrices = loadCurrentPrices(figies);
            Map<String, BigDecimal> baselinePrices = priceBaselineService.resolveBaselinePrices(figies);
            DailyChangeResult dailyChange = dailyChangeCalculator.calculate(positions, currentPrices, baselinePrices);

            List<AssetDto> assets = positions.stream()
                    .map(position -> buildAsset(position, stockMetadata, currentPrices))
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
                    dailyChange.dailyChange(),
                    dailyChange.dailyChangePercent()
            );
        } catch (Exception e) {
            log.error("Ошибка при получении дашборда для пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Не удалось получить данные дашборда", e);
        }
    }

    private List<PositionSnapshot> resolvePositions(String userId) {
        List<JournalPositionDto> journalPositions = journalClient.getPositions(userId);
        if (journalPositions != null && !journalPositions.isEmpty()) {
            return journalPositions.stream()
                    .map(position -> new PositionSnapshot(position.figi(), position.quantity()))
                    .toList();
        }

        log.info("Journal пуст для пользователя {}, fallback на UserService positions", userId);
        return userServiceClient.getUserPositions(userId).stream()
                .map(position -> new PositionSnapshot(position.getFigi(), position.getQuantity()))
                .toList();
    }

    private Map<String, StockDto> loadStockMetadata(List<String> figies) {
        if (figies.isEmpty()) {
            return Map.of();
        }

        try {
            return userServiceClient.getStocksByFigis(new FigisRequest(figies)).stream()
                    .collect(Collectors.toMap(StockDto::figi, stock -> stock, (left, right) -> left));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метаданные акций: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, BigDecimal> loadCurrentPrices(List<String> figies) {
        if (figies.isEmpty()) {
            return Map.of();
        }

        return priceServiceClient.getPrices(figies).stream()
                .collect(Collectors.toMap(PriceDataDto::getFigi, PriceDataDto::getPrice, (left, right) -> left));
    }

    private AssetDto buildAsset(
            PositionSnapshot position,
            Map<String, StockDto> stockMetadata,
            Map<String, BigDecimal> currentPrices) {

        StockDto stock = stockMetadata.get(position.figi());
        String ticker = stock != null ? stock.ticker() : position.figi();
        String name = stock != null ? stock.name() : position.figi();
        String currency = stock != null ? stock.currency() : "RUB";
        BigDecimal currentPrice = currentPrices.getOrDefault(position.figi(), BigDecimal.ZERO);
        BigDecimal totalValue = currentPrice.multiply(position.quantity());

        return new AssetDto(
                position.figi(),
                ticker,
                name,
                position.quantity(),
                currentPrice,
                totalValue,
                currency
        );
    }
}
