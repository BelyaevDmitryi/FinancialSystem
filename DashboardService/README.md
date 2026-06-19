# Dashboard Service

Сервис дашборда для отображения активов и статистики пользователя.

## Порт

- **8009** — основной порт Dashboard Service

## Функциональность

### Дашборд пользователя

- `GET /dashboard` — дашборд текущего пользователя
  - Общая стоимость портфеля
  - Дневное изменение (`dailyChange`, `dailyChangePercent`)
  - Количество позиций
  - Количество активных ботов
  - Количество ожидающих ордеров
  - Список активов с ticker, name, currency и текущими ценами

Идентификатор пользователя берётся из заголовка `X-User-Id`, который Gateway устанавливает из JWT. **Параметра `{userId}` в path нет.**

### Источники данных

| Данные | Первичный источник | Fallback |
|--------|-------------------|----------|
| Позиции | `JournalService` (`/journal/positions`) | `UserService` (`/users/{id}/positions`, read-only) |
| Текущие цены | `PriceService` (`/prices`) | — |
| Baseline (previous close) | config `dashboard.price-baseline` | daily change = 0 + WARN |
| Метаданные акций | `UserService` (`/stocks/by-figis`) | figi как ticker/name |

### Конфигурация baseline цен

```yaml
dashboard:
  price-baseline: REDIS_SNAPSHOT   # или MARKET_HISTORY_D1
```

- `REDIS_SNAPSHOT` — EOD snapshot из Redis-кэша `PriceService` (`GET /prices/snapshot`)
- `MARKET_HISTORY_D1` — close предыдущего D1-бара из `MarketHistoryService`

При недоступности `MarketHistoryService` (P0 compose) сервис логирует WARN и возвращает `dailyChange = 0` (без HTTP 500).

### Формула дневного изменения

```
dailyChange = currentValue - valueAtPreviousClose
dailyChangePercent = dailyChange / valueAtPreviousClose * 100
```

где `currentValue = Σ(quantity × currentPrice)`, `valueAtPreviousClose = Σ(quantity × baselinePrice)`.

## Примеры использования

### Через API Gateway (рекомендуется)

```bash
GET http://localhost:8090/api/dashboard
Authorization: Bearer <token>
```

### Прямой вызов (отладка)

```bash
GET http://localhost:8009/dashboard
X-User-Id: 42
```

## Health Check

```bash
GET http://localhost:8009/actuator/health
```

## Зависимости (Feign)

- `journal-service` — позиции из журнала сделок
- `user-service` — fallback позиции и метаданные акций
- `price-service` — текущие и snapshot цены
- `market-history-service` — D1 baseline (опционально, P1 compose)
- `trading-terminal-service` — ордера пользователя
- `trading-bot-service` — боты пользователя
