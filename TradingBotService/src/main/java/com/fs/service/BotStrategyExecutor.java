package com.fs.service;

import com.fs.domain.BotStrategy;
import com.fs.domain.TradingBot;
import com.fs.dto.*;
import com.fs.feignclient.AnalyticsServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotStrategyExecutor {

    private final AnalyticsServiceClient analyticsServiceClient;

    public boolean shouldTrade(TradingBot bot, List<PriceDataDto> prices) {
        try {
            return switch (bot.getStrategy()) {
                case MACD_CROSSOVER -> checkMacdCrossover(bot, prices);
                case SMA_CROSSOVER -> checkSmaCrossover(bot, prices);
                case VOLATILITY_BREAKOUT -> checkVolatilityBreakout(bot, prices);
                case EMA_TREND -> checkEmaTrend(bot, prices);
            };
        } catch (Exception e) {
            log.error("Ошибка при выполнении стратегии {} для бота {}: {}", bot.getStrategy(), bot.getId(), e.getMessage());
            return false;
        }
    }

    private boolean checkMacdCrossover(TradingBot bot, List<PriceDataDto> prices) {
        if (prices.size() < 26) {
            return false;
        }

        AnalyticsRequestDto request = new AnalyticsRequestDto(bot.getFigi(), prices, null);
        MacdResponseDto macd = analyticsServiceClient.calculateMACD(request);

        // Покупка: MACD пересекает сигнальную линию снизу вверх
        return macd.getHistogram().compareTo(BigDecimal.ZERO) > 0 && 
               macd.getMacd().compareTo(macd.getSignal()) > 0;
    }

    private boolean checkSmaCrossover(TradingBot bot, List<PriceDataDto> prices) {
        if (bot.getSmaPeriod() == null || prices.size() < bot.getSmaPeriod()) {
            return false;
        }

        AnalyticsRequestDto request = new AnalyticsRequestDto(bot.getFigi(), prices, bot.getSmaPeriod());
        SmaResponseDto sma = analyticsServiceClient.calculateSMA(request);
        
        BigDecimal currentPrice = prices.get(prices.size() - 1).getPrice();
        
        // Покупка: цена выше SMA
        return currentPrice.compareTo(sma.getSma()) > 0;
    }

    private boolean checkVolatilityBreakout(TradingBot bot, List<PriceDataDto> prices) {
        if (prices.size() < 20) {
            return false;
        }

        AnalyticsRequestDto request = new AnalyticsRequestDto(bot.getFigi(), prices, 20);
        VolatilityResponseDto volatility = analyticsServiceClient.calculateVolatility(request);

        // Покупка при высокой волатильности (пробой)
        return volatility.getVolatility().compareTo(BigDecimal.valueOf(5)) > 0;
    }

    private boolean checkEmaTrend(TradingBot bot, List<PriceDataDto> prices) {
        if (bot.getEmaPeriod() == null || prices.size() < bot.getEmaPeriod()) {
            return false;
        }

        AnalyticsRequestDto request = new AnalyticsRequestDto(bot.getFigi(), prices, bot.getEmaPeriod());
        EmaResponseDto ema = analyticsServiceClient.calculateEMA(request);
        
        BigDecimal currentPrice = prices.get(prices.size() - 1).getPrice();
        
        // Покупка: цена выше EMA (восходящий тренд)
        return currentPrice.compareTo(ema.getEma()) > 0;
    }
}
