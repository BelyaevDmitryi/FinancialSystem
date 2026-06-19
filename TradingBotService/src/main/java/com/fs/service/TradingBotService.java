package com.fs.service;

import com.fs.candle.CandleHistoryProvider;
import com.fs.config.FsBotProperties;
import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import com.fs.domain.TradingBot;
import com.fs.dto.*;
import com.fs.feignclient.JournalClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.repository.TradingBotRepository;
import com.fs.strategy.DefaultStrategyContext;
import com.fs.strategy.StrategyContext;
import com.fs.trading.core.TradeSignal;
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
import java.util.Objects;
import java.util.stream.Collectors;

import feign.FeignException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingBotService {

    private static final String MARKET_ORDER_TYPE = "MARKET";

    private final TradingBotRepository botRepository;
    private final CandleHistoryProvider candleHistoryProvider;
    private final TradingTerminalServiceClient terminalServiceClient;
    private final JournalClient journalClient;
    private final BotStrategyExecutor strategyExecutor;
    private final FsBotProperties fsBotProperties;

    @Transactional
    public BotDto createBot(String userId, CreateBotDto createBotDto) {
        log.info("Создание бота для пользователя: {}, FIGI: {}, стратегия: {}", userId, createBotDto.getFigi(), createBotDto.getStrategy());

        if (BotStrategy.SMA_CROSSOVER.equals(createBotDto.getStrategy())
                && (createBotDto.getSmaPeriod() == null || createBotDto.getSmaPeriod() <= 0)) {
            throw new IllegalArgumentException("smaPeriod обязателен для стратегии SMA_CROSSOVER");
        }
        if (BotStrategy.EMA_TREND.equals(createBotDto.getStrategy())
                && (createBotDto.getEmaPeriod() == null || createBotDto.getEmaPeriod() <= 0)) {
            throw new IllegalArgumentException("emaPeriod обязателен для стратегии EMA_TREND");
        }

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
        bot.setPaper(resolvePaperFlag(createBotDto));
        bot.setCreatedAt(LocalDateTime.now());
        bot.setTotalTrades(0);
        bot.setTotalProfit(BigDecimal.ZERO);

        TradingBot savedBot = botRepository.save(bot);
        log.info("Бот создан с ID: {}", savedBot.getId());

        return convertToDto(savedBot);
    }

    public List<BotDto> getUserBots(String userId) {
        Long userIdLong = parseUserId(userId);
        List<TradingBot> bots = botRepository.findByUserId(userIdLong);
        bots.forEach(this::syncTotalProfitFromJournal);
        botRepository.saveAll(bots);
        return bots.stream()
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
        log.info("Обновление статуса бота: id={}, userId={}, figi={}, strategy={}, newStatus={}",
                botIdLong, userIdLong, bot.getFigi(), bot.getStrategy(), status);

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
        try {
            List<PriceDataDto> candles = candleHistoryProvider.getCandles(bot.getFigi());
            if (candles.isEmpty()) {
                log.warn("Нет данных о ценах для FIGI: {}", bot.getFigi());
                return;
            }

            BigDecimal currentPrice = candles.get(candles.size() - 1).getPrice();

            if (bot.getMinPrice() != null && currentPrice.compareTo(bot.getMinPrice()) < 0) {
                log.debug("Цена {} ниже минимальной {} для бота {}", currentPrice, bot.getMinPrice(), bot.getId());
                return;
            }

            if (bot.getMaxPrice() != null && currentPrice.compareTo(bot.getMaxPrice()) > 0) {
                log.debug("Цена {} выше максимальной {} для бота {}", currentPrice, bot.getMaxPrice(), bot.getId());
                return;
            }

            BigDecimal currentQuantity = resolveCurrentPositionQuantity(bot);
            StrategyContext context = new DefaultStrategyContext(candles, currentQuantity);
            TradeSignal signal = strategyExecutor.evaluate(bot, context);

            if (signal == TradeSignal.HOLD) {
                return;
            }

            if (signal == TradeSignal.BUY) {
                if (currentQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("Пропуск BUY: позиция уже открыта для бота {}", bot.getId());
                    return;
                }
                BigDecimal maxQuantity = bot.getMaxPositionSize().divide(currentPrice, 2, RoundingMode.DOWN);
                if (maxQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("Нулевой размер позиции для бота {}", bot.getId());
                    return;
                }
                placeOrder(bot, com.fs.domain.OrderType.BUY, maxQuantity, currentPrice);
                return;
            }

            if (signal == TradeSignal.SELL && currentQuantity.compareTo(BigDecimal.ZERO) > 0) {
                placeOrder(bot, com.fs.domain.OrderType.SELL, currentQuantity, currentPrice);
            }
        } finally {
            syncTotalProfitFromJournal(bot);
            botRepository.save(bot);
        }
    }

    private void placeOrder(TradingBot bot, com.fs.domain.OrderType orderType,
                            BigDecimal quantity, BigDecimal currentPrice) {
        CreateOrderDto orderDto = new CreateOrderDto();
        orderDto.setFigi(bot.getFigi());
        orderDto.setType(orderType);
        orderDto.setPrice(currentPrice);
        orderDto.setQuantity(quantity);
        orderDto.setOrderType(MARKET_ORDER_TYPE);
        orderDto.setPaper(bot.isPaper());
        orderDto.setComment("Автоматическая торговля ботом: " + bot.getName());

        try {
            OrderDto order = terminalServiceClient.createOrder(String.valueOf(bot.getUserId()), orderDto);
            log.info("Бот {} создал ордер {}: {}", bot.getId(), orderType, order.getId());

            bot.setLastExecution(LocalDateTime.now());
            bot.setTotalTrades(bot.getTotalTrades() + 1);
            botRepository.save(bot);
        } catch (FeignException.Unauthorized e) {
            log.error("Ошибка авторизации (401) при создании ордера ботом {}: {}. Проверьте S2S JWT конфигурацию.",
                    bot.getId(), e.getMessage());
        } catch (FeignException e) {
            log.error("Ошибка Feign при создании ордера ботом {} (HTTP {}): {}", bot.getId(), e.status(), e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при создании ордера ботом {}: {}", bot.getId(), e.getMessage());
        }
    }

    private BigDecimal resolveCurrentPositionQuantity(TradingBot bot) {
        try {
            JournalPositionDto position = journalClient.getPosition(bot.getUserId(), bot.getFigi());
            return position != null && position.quantity() != null
                    ? position.quantity()
                    : BigDecimal.ZERO;
        } catch (FeignException.NotFound e) {
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Не удалось получить позицию из Journal для бота {}: {}", bot.getId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void syncTotalProfitFromJournal(TradingBot bot) {
        bot.setTotalProfit(resolveTotalProfitFromJournal(bot));
    }

    private BigDecimal resolveTotalProfitFromJournal(TradingBot bot) {
        try {
            List<JournalTradeDto> trades = journalClient.getTrades(bot.getUserId());
            if (trades == null || trades.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return trades.stream()
                    .filter(trade -> bot.getFigi().equals(trade.figi()))
                    .map(JournalTradeDto::realizedPnl)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Не удалось получить realized PnL из Journal для бота {}: {}", bot.getId(), e.getMessage());
            return bot.getTotalProfit() != null ? bot.getTotalProfit() : BigDecimal.ZERO;
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
                bot.getTotalProfit(),
                bot.isPaper()
        );
    }

    private boolean resolvePaperFlag(CreateBotDto createBotDto) {
        return createBotDto.getPaper() != null
                ? createBotDto.getPaper()
                : fsBotProperties.isDefaultPaper();
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
