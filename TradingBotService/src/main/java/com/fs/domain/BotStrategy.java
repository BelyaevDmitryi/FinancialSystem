package com.fs.domain;

public enum BotStrategy {
    MACD_CROSSOVER,      // Пересечение MACD
    SMA_CROSSOVER,       // Пересечение SMA
    VOLATILITY_BREAKOUT, // Пробой волатильности
    EMA_TREND            // Тренд по EMA
}
