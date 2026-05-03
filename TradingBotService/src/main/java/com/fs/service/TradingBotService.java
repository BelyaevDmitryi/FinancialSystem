package com.fs.service;

import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import com.fs.domain.TradingBot;
import com.fs.dto.*;
import com.fs.feignclient.AnalyticsServiceClient;
import com.fs.feignclient.PriceServiceClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.repository.TradingBotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingBotService {

    private final TradingBotRepository botRepository;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final PriceServiceClient priceServiceClient;
    private final TradingTerminalServiceClient terminalServiceClient;
    private final BotStrategyExecutor strategyExecutor;

    @Transactional
    public BotDto createBot(String userId, CreateBotDto createBotDto) {
        log.info("Создание бота для пользователя: {}, FIGI: {}, стратегия: {}", userId, createBotDto.getFigi(), createBotDto.getStrategy());
        
        Long userIdLong = parseUserId(userId);
        
        TradingBot bot = new TradingBot();
        bot.setUserId(userIdLong);
        bot.setFigi(createBotDto.getFigi());
        bot.setName(createBotDto.getName());
        bot.setStrategy(createBotDto.getStrategy());
        bot.setStatus(BotStatus.ACTIVE);
        bot.setMaxPositionSize(createBotDto.getMaxPositionSize());
        bot.setMinPrice(createBotDto.getMinPrice());
        bot.setMaxPrice(createBotDto.getMaxPrice());
        bot.setSmaPeriod(createBotDto.getSmaPeriod());
        bot.setEmaPeriod(createBotDto.getEmaPeriod());
        bot.setCreatedAt(LocalDateTime.now());
        bot.setTotalTrades(0);
        bot.setTotalProfit(BigDecimal.ZERO);

        TradingBot savedBot = botRepository.save(bot);
        log.info("Бот создан с ID: {}", savedBot.getId());
        
        return convertToDto(savedBot);
    }

    public List<BotDto> getUserBots(String userId) {
        Long userIdLong = parseUserId(userId);
        return botRepository.findByUserId(userIdLong).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BotDto updateBotStatus(String userId, String botId, BotStatus status) {
        Long userIdLong = parseUserId(userId);
        Long botIdLong = parseBotId(botId);
        TradingBot bot = botRepository.findById(botIdLong)
                .orElseThrow(() -> new IllegalArgumentException("Бот не найден: " + botId));
        if (!bot.getUserId().equals(userIdLong)) {
            throw new IllegalArgumentException("Нет доступа к боту: " + botId);
        }

        bot.setStatus(status);
        TradingBot savedBot = botRepository.save(bot);
        log.info("Статус бота {} изменен на {}", botId, status);

        return convertToDto(savedBot);
    }

    @Transactional
    public void deleteBot(String userId, String botId) {
        Long userIdLong = parseUserId(userId);
        Long botIdLong = parseBotId(botId);
        TradingBot bot = botRepository.findById(botIdLong)
                .orElseThrow(() -> new IllegalArgumentException("Бот не найден: " + botId));
        if (!bot.getUserId().equals(userIdLong)) {
            throw new IllegalArgumentException("Нет доступа к боту: " + botId);
        }
        botRepository.delete(bot);
        log.info("Бот {} удалён пользователем {}", botId, userIdLong);
    }

    @Scheduled(fixedDelayString = "${bot.scheduler.fixed-delay}")
    public void executeBots() {
        log.debug("Запуск выполнения активных ботов");
        List<TradingBot> activeBots = botRepository.findByStatus(BotStatus.ACTIVE);
        
        for (TradingBot bot : activeBots) {
            try {
                executeBot(bot);
            } catch (Exception e) {
                log.error("Ошибка при выполнении бота {}: {}", bot.getId(), e.getMessage(), e);
            }
        }
    }

    private void executeBot(TradingBot bot) {
        log.debug("Выполнение бота: {}", bot.getId());
        
        List<PriceDataDto> prices = priceServiceClient.getPrices(List.of(bot.getFigi()));
        if (prices.isEmpty()) {
            log.warn("Нет данных о ценах для FIGI: {}", bot.getFigi());
            return;
        }

        BigDecimal currentPrice = prices.get(0).getPrice();
        
        if (bot.getMinPrice() != null && currentPrice.compareTo(bot.getMinPrice()) < 0) {
            log.debug("Цена {} ниже минимальной {} для бота {}", currentPrice, bot.getMinPrice(), bot.getId());
            return;
        }
        
        if (bot.getMaxPrice() != null && currentPrice.compareTo(bot.getMaxPrice()) > 0) {
            log.debug("Цена {} выше максимальной {} для бота {}", currentPrice, bot.getMaxPrice(), bot.getId());
            return;
        }

        boolean shouldTrade = strategyExecutor.shouldTrade(bot, prices);
        
        if (shouldTrade) {
            CreateOrderDto orderDto = new CreateOrderDto();
            orderDto.setFigi(bot.getFigi());
            orderDto.setType(com.fs.domain.OrderType.BUY);
            orderDto.setPrice(currentPrice);
            orderDto.setQuantity(bot.getMaxPositionSize().divide(currentPrice, 2, RoundingMode.DOWN));
            orderDto.setComment("Автоматическая торговля ботом: " + bot.getName());
            
            try {
                OrderDto order = terminalServiceClient.createOrder(String.valueOf(bot.getUserId()), orderDto);
                log.info("Бот {} создал ордер: {}", bot.getId(), order.getId());
                
                bot.setLastExecution(LocalDateTime.now());
                bot.setTotalTrades(bot.getTotalTrades() + 1);
                botRepository.save(bot);
            } catch (Exception e) {
                log.error("Ошибка при создании ордера ботом {}: {}", bot.getId(), e.getMessage());
            }
        }
    }

    public BotStatsDto getBotStats() {
        List<TradingBot> allBots = botRepository.findAll();
        Long activeBots = allBots.stream()
                .filter(bot -> bot.getStatus() == BotStatus.ACTIVE)
                .count();
        
        Map<String, Long> botsByStrategy = allBots.stream()
                .collect(Collectors.groupingBy(
                        bot -> bot.getStrategy().name(),
                        Collectors.counting()
                ));
        
        return new BotStatsDto(activeBots, botsByStrategy);
    }

    private BotDto convertToDto(TradingBot bot) {
        return new BotDto(
                String.valueOf(bot.getId()),
                String.valueOf(bot.getUserId()),
                bot.getFigi(),
                bot.getName(),
                bot.getStrategy(),
                bot.getStatus(),
                bot.getMaxPositionSize(),
                bot.getMinPrice(),
                bot.getMaxPrice(),
                bot.getSmaPeriod(),
                bot.getEmaPeriod(),
                bot.getCreatedAt(),
                bot.getLastExecution(),
                bot.getTotalTrades(),
                bot.getTotalProfit()
        );
    }
    
    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат userId: " + userId);
        }
    }
    
    private Long parseBotId(String botId) {
        try {
            return Long.parseLong(botId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат botId: " + botId);
        }
    }
}
