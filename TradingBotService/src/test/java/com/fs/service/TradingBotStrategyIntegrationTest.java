package com.fs.service;

import com.fs.config.FsBotProperties;
import com.fs.candle.CandleHistoryProvider;
import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.domain.TradingBot;
import com.fs.dto.JournalPositionDto;
import com.fs.dto.OrderDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.JournalClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.repository.TradingBotRepository;
import com.fs.strategy.*;
import com.fs.trading.core.EmaTrendStrategy;
import com.fs.trading.core.MacdCrossoverStrategy;
import com.fs.trading.core.SmaCrossoverStrategy;
import com.fs.trading.core.VolatilityBreakoutStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * US-OSE-002 M2 / US-OSE-007 T2 / US-OSE-008: round-trip BUY→SELL без Feign Analytics.
 */
@ExtendWith(MockitoExtension.class)
class TradingBotStrategyIntegrationTest {

    @Mock
    private TradingBotRepository botRepository;

    @Mock
    private CandleHistoryProvider candleHistoryProvider;

    @Mock
    private TradingTerminalServiceClient terminalServiceClient;

    @Mock
    private JournalClient journalClient;

    private final FsBotProperties fsBotProperties = new FsBotProperties();

    private TradingBotService tradingBotService;

    @BeforeEach
    void setUp() {
        BotStrategyExecutor strategyExecutor = new BotStrategyExecutor(
                new SmaCrossoverStrategyAdapter(new SmaCrossoverStrategy()),
                new MacdCrossoverStrategyAdapter(new MacdCrossoverStrategy()),
                new EmaTrendStrategyAdapter(new EmaTrendStrategy()),
                new VolatilityBreakoutStrategyAdapter(new VolatilityBreakoutStrategy())
        );
        tradingBotService = new TradingBotService(
                botRepository,
                candleHistoryProvider,
                terminalServiceClient,
                journalClient,
                strategyExecutor,
                fsBotProperties
        );
    }

    @Test
    void executeBots_smaBuyThenSell_roundTrip() {
        TradingBot bot = new TradingBot();
        bot.setId(1L);
        bot.setUserId(42L);
        bot.setFigi("BBG004730N88");
        bot.setName("IntegrationTestBot");
        bot.setStrategy(BotStrategy.SMA_CROSSOVER);
        bot.setStatus(BotStatus.ACTIVE);
        bot.setSmaPeriod(20);
        bot.setMaxPositionSize(BigDecimal.valueOf(1000));
        bot.setTotalTrades(0);
        bot.setTotalProfit(BigDecimal.ZERO);
        bot.setCreatedAt(LocalDateTime.now());

        List<PriceDataDto> buyCandles = buildCandles(20, BigDecimal.valueOf(100));
        List<PriceDataDto> sellCandles = buildDecliningCandles(20, BigDecimal.valueOf(120));

        OrderDto buyOrder = new OrderDto("201", "42", "BBG004730N88",
                OrderType.BUY, BigDecimal.valueOf(8), BigDecimal.valueOf(119),
                OrderStatus.PENDING, LocalDateTime.now(), null, null);

        OrderDto sellOrder = new OrderDto("202", "42", "BBG004730N88",
                OrderType.SELL, BigDecimal.valueOf(8), BigDecimal.valueOf(119),
                OrderStatus.PENDING, LocalDateTime.now(), null, null);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88"))
                .thenReturn(buyCandles)
                .thenReturn(sellCandles);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ZERO, BigDecimal.ZERO))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.valueOf(8), BigDecimal.valueOf(119)));
        when(journalClient.getTrades(42L)).thenReturn(List.of());
        when(terminalServiceClient.createOrder(eq("42"), any()))
                .thenReturn(buyOrder)
                .thenReturn(sellOrder);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();
        tradingBotService.executeBots();

        verify(terminalServiceClient, times(1)).createOrder(eq("42"), argThat(order ->
                order.getType() == OrderType.BUY && "MARKET".equals(order.getOrderType())));
        verify(terminalServiceClient, times(1)).createOrder(eq("42"), argThat(order ->
                order.getType() == OrderType.SELL && "MARKET".equals(order.getOrderType())));
    }

    private static List<PriceDataDto> buildCandles(int count, BigDecimal startPrice) {
        List<PriceDataDto> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            prices.add(new PriceDataDto("BBG004730N88", startPrice.add(BigDecimal.valueOf(i)),
                    LocalDateTime.now().minusMinutes(count - i)));
        }
        return prices;
    }

    private static List<PriceDataDto> buildDecliningCandles(int count, BigDecimal startPrice) {
        List<PriceDataDto> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            prices.add(new PriceDataDto("BBG004730N88", startPrice.subtract(BigDecimal.valueOf(i)),
                    LocalDateTime.now().minusMinutes(count - i)));
        }
        return prices;
    }
}
