# Trading Bot Service

Сервис для автоматической торговли на основе технического анализа.

## Порт
- **8007** - основной порт Trading Bot Service

## Функциональность

### Управление ботами

1. **Создание бота**
   - POST `/bots`
   - Требует заголовок `X-User-Id`
   - Поддерживает стратегии: MACD_CROSSOVER, SMA_CROSSOVER, VOLATILITY_BREAKOUT, EMA_TREND

2. **Управление ботами**
   - GET `/bots` - получить все боты пользователя
   - PUT `/bots/{botId}/status` - изменить статус бота

### Стратегии торговли

- **MACD_CROSSOVER** - покупка при пересечении MACD сигнальной линии
- **SMA_CROSSOVER** - покупка когда цена выше SMA
- **VOLATILITY_BREAKOUT** - покупка при высокой волатильности
- **EMA_TREND** - покупка при восходящем тренде по EMA

### Автоматическое выполнение

Боты автоматически выполняются каждую минуту (настраивается через `bot.scheduler.fixed-delay`).

## Примеры использования

### Создание бота с MACD стратегией
```bash
POST http://localhost:8007/bots
X-User-Id: user123
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "name": "MACD Bot",
  "strategy": "MACD_CROSSOVER",
  "maxPositionSize": 100000,
  "minPrice": 2000,
  "maxPrice": 3000
}
```

### Изменение статуса бота
```bash
PUT http://localhost:8007/bots/{botId}/status?status=PAUSED
```

## Health Check
```bash
GET http://localhost:8007/actuator/health
```
