package com.fs.service;

import com.fs.config.FsBotProperties;
import com.fs.candle.CandleHistoryProvider;
import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import com.fs.domain.OrderStatus;
import com.fs.domain.OrderType;
import com.fs.domain.TradingBot;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.JournalPositionDto;
import com.fs.dto.JournalTradeDto;
import com.fs.dto.OrderDto;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.JournalClient;
import com.fs.feignclient.TradingTerminalServiceClient;
import com.fs.repository.TradingBotRepository;
import com.fs.trading.core.TradeSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradingBotExecutorTest {

    @Mock
    private TradingBotRepository botRepository;

    @Mock
    private CandleHistoryProvider candleHistoryProvider;

    @Mock
    private TradingTerminalServiceClient terminalServiceClient;

    @Mock
    private JournalClient journalClient;

    @Mock
    private BotStrategyExecutor strategyExecutor;

    private final FsBotProperties fsBotProperties = new FsBotProperties();

    private TradingBotService tradingBotService;

    @BeforeEach
    void initService() {
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
    void executeBots_paperBot_passesPaperFlagToTerminal() {
        TradingBot bot = activeBot();
        bot.setPaper(true);
        List<PriceDataDto> candles = buildCandles(20);

        OrderDto mockOrder = new OrderDto("101", "42", "BBG004730N88",
                OrderType.BUY, BigDecimal.TEN, BigDecimal.valueOf(119),
                OrderStatus.PENDING, LocalDateTime.now(), null, null);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ZERO, BigDecimal.ZERO));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.BUY);
        when(terminalServiceClient.createOrder(eq("42"), any())).thenReturn(mockOrder);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();

        ArgumentCaptor<CreateOrderDto> orderCaptor = ArgumentCaptor.forClass(CreateOrderDto.class);
        verify(terminalServiceClient, times(1)).createOrder(eq("42"), orderCaptor.capture());
        assertThat(orderCaptor.getValue().getPaper()).isTrue();
    }

    @Test
    void executeBots_liveBot_passesPaperFalseToTerminal() {
        TradingBot bot = activeBot();
        bot.setPaper(false);
        List<PriceDataDto> candles = buildCandles(20);

        OrderDto mockOrder = new OrderDto("101", "42", "BBG004730N88",
                OrderType.BUY, BigDecimal.TEN, BigDecimal.valueOf(119),
                OrderStatus.PENDING, LocalDateTime.now(), null, null);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ZERO, BigDecimal.ZERO));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.BUY);
        when(terminalServiceClient.createOrder(eq("42"), any())).thenReturn(mockOrder);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();

        ArgumentCaptor<CreateOrderDto> orderCaptor = ArgumentCaptor.forClass(CreateOrderDto.class);
        verify(terminalServiceClient).createOrder(eq("42"), orderCaptor.capture());
        assertThat(orderCaptor.getValue().getPaper()).isFalse();
    }

    @Test
    void executeBots_buySignalWithoutPosition_callsCreateBuyOrder() {
        TradingBot bot = activeBot();
        List<PriceDataDto> candles = buildCandles(20);

        OrderDto mockOrder = new OrderDto("101", "42", "BBG004730N88",
                OrderType.BUY, BigDecimal.TEN, BigDecimal.valueOf(119),
                OrderStatus.PENDING, LocalDateTime.now(), null, null);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ZERO, BigDecimal.ZERO));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.BUY);
        when(terminalServiceClient.createOrder(eq("42"), any())).thenReturn(mockOrder);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();

        ArgumentCaptor<CreateOrderDto> orderCaptor = ArgumentCaptor.forClass(CreateOrderDto.class);
        verify(terminalServiceClient, times(1)).createOrder(eq("42"), orderCaptor.capture());
        assertThat(orderCaptor.getValue().getType()).isEqualTo(OrderType.BUY);
        assertThat(orderCaptor.getValue().getOrderType()).isEqualTo("MARKET");
        verify(botRepository, atLeastOnce()).save(any(TradingBot.class));
    }

    @Test
    void executeBots_sellSignalWithPosition_createsSellOrder() {
        TradingBot bot = activeBot();
        List<PriceDataDto> candles = buildCandles(20);
        BigDecimal positionQty = BigDecimal.valueOf(5);

        OrderDto mockOrder = new OrderDto("102", "42", "BBG004730N88",
                OrderType.SELL, positionQty, BigDecimal.valueOf(119),
                OrderStatus.PENDING, LocalDateTime.now(), null, null);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", positionQty, BigDecimal.valueOf(100)));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.SELL);
        when(terminalServiceClient.createOrder(eq("42"), any())).thenReturn(mockOrder);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();

        ArgumentCaptor<CreateOrderDto> orderCaptor = ArgumentCaptor.forClass(CreateOrderDto.class);
        verify(terminalServiceClient, times(1)).createOrder(eq("42"), orderCaptor.capture());
        assertThat(orderCaptor.getValue().getType()).isEqualTo(OrderType.SELL);
        assertThat(orderCaptor.getValue().getQuantity()).isEqualByComparingTo(positionQty);
        assertThat(orderCaptor.getValue().getOrderType()).isEqualTo("MARKET");
    }

    @Test
    void executeBots_noCandles_doesNotCallCreateOrder() {
        TradingBot bot = activeBot();

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(List.of());
        stubJournalDefaults(bot);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();

        verify(terminalServiceClient, never()).createOrder(anyString(), any());
        verify(botRepository).save(bot);
    }

    @Test
    void executeBots_syncsTotalProfitFromJournalTrades() {
        TradingBot bot = activeBot();
        List<PriceDataDto> candles = buildCandles(20);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ZERO, BigDecimal.ZERO));
        when(journalClient.getTrades(42L)).thenReturn(List.of(
                journalTrade(bot, BigDecimal.valueOf(50)),
                journalTrade(bot, BigDecimal.valueOf(30)),
                journalTrade(bot, "OTHER_FIGI", BigDecimal.valueOf(999))
        ));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.HOLD);
        when(botRepository.save(any(TradingBot.class))).thenReturn(bot);

        tradingBotService.executeBots();

        ArgumentCaptor<TradingBot> botCaptor = ArgumentCaptor.forClass(TradingBot.class);
        verify(botRepository).save(botCaptor.capture());
        assertThat(botCaptor.getValue().getTotalProfit()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void executeBots_holdSignal_doesNotCallCreateOrder() {
        TradingBot bot = activeBot();
        List<PriceDataDto> candles = buildCandles(20);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.ZERO, BigDecimal.ZERO));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.HOLD);

        tradingBotService.executeBots();

        verify(terminalServiceClient, never()).createOrder(anyString(), any());
    }

    @Test
    void executeBots_buySignalWithOpenPosition_doesNotDuplicateBuy() {
        TradingBot bot = activeBot();
        List<PriceDataDto> candles = buildCandles(20);

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.TEN, BigDecimal.valueOf(100)));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.BUY);

        tradingBotService.executeBots();

        verify(terminalServiceClient, never()).createOrder(anyString(), any());
    }

    @Test
    void executeBots_positionAtMaxWithBuySignal_doesNotCallCreateOrder() {
        TradingBot bot = activeBot();
        List<PriceDataDto> candles = List.of(
                new PriceDataDto("BBG004730N88", BigDecimal.valueOf(100), LocalDateTime.now()));

        when(botRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(bot));
        when(candleHistoryProvider.getCandles("BBG004730N88")).thenReturn(candles);
        stubJournalDefaults(bot);
        when(journalClient.getPosition(42L, "BBG004730N88"))
                .thenReturn(new JournalPositionDto(42L, "BBG004730N88", BigDecimal.TEN, BigDecimal.valueOf(100)));
        when(strategyExecutor.evaluate(eq(bot), any())).thenReturn(TradeSignal.BUY);

        tradingBotService.executeBots();

        verify(terminalServiceClient, never()).createOrder(anyString(), any());
    }

    private void stubJournalDefaults(TradingBot bot) {
        when(journalClient.getTrades(bot.getUserId())).thenReturn(Collections.emptyList());
    }

    private static JournalTradeDto journalTrade(TradingBot bot, BigDecimal realizedPnl) {
        return journalTrade(bot, bot.getFigi(), realizedPnl);
    }

    private static JournalTradeDto journalTrade(TradingBot bot, String figi, BigDecimal realizedPnl) {
        return new JournalTradeDto(
                1L,
                bot.getUserId(),
                figi,
                "SELL",
                BigDecimal.ONE,
                BigDecimal.valueOf(100),
                realizedPnl,
                100L,
                BigDecimal.ZERO,
                LocalDateTime.now()
        );
    }

    private static TradingBot activeBot() {
        TradingBot bot = new TradingBot();
        bot.setId(1L);
        bot.setUserId(42L);
        bot.setFigi("BBG004730N88");
        bot.setName("TestBot");
        bot.setStrategy(BotStrategy.SMA_CROSSOVER);
        bot.setStatus(BotStatus.ACTIVE);
        bot.setSmaPeriod(20);
        bot.setMaxPositionSize(BigDecimal.valueOf(1000));
        bot.setTotalTrades(0);
        bot.setTotalProfit(BigDecimal.ZERO);
        bot.setCreatedAt(LocalDateTime.now());
        return bot;
    }

    private static List<PriceDataDto> buildCandles(int count) {
        List<PriceDataDto> prices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            prices.add(new PriceDataDto("BBG004730N88", BigDecimal.valueOf(100 + i),
                    LocalDateTime.now().minusMinutes(count - i)));
        }
        return prices;
    }
}
