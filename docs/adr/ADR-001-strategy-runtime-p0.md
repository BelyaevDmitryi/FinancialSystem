# ADR-001: Strategy Runtime Contract (P0)

**Status:** Accepted  
**Date:** 2026-05-23  
**Deciders:** Product / architecture (sign-off open questions OsEngine parity)  
**Related plans:** EPIC-OSE-000, US-OSE-002, US-OSE-003, US-OSE-005  
**Amended by:** [ADR-002 Integration Decisions](ADR-002-integration-decisions.md) (2026-05-23)

## Context

OsEngine выполняет стратегии **in-process**: `BotPanel` подписывается на `CandleFinishedEvent` вкладки `BotTabSimple` и вызывает `BuyAtMarket` / `SellAtMarket` через `ConnectorCandles`.

FinancialSystem — **микросервисы** с JWT/Feign. Нужен минимальный контракт для P0 (live-боты), согласованный с отдельным `JournalService` и пакетом `com.fs.backtest` в `BotOptimizationService` (P1).

## Decision

### 1. Владелец runtime API

| Компонент | Сервис / пакет |
|-----------|----------------|
| `TradingStrategy`, `TradeSignal`, `BotStrategyExecutor` | `TradingBotService` (`com.fs.strategy`) |
| `StrategyContext` interface | `TradingBotService` |
| `DefaultStrategyContext` (Feign) | `TradingBotService` |
| Ордера | `TradingTerminalService` (REST/Feign) |
| Позиции / PnL | **`JournalService`** (REST/Feign) |
| Свечи lookback | P0: **`PriceService`** + Analytics; P1+: **`MarketHistoryService`** DB-first, fallback Price ([ADR-002](ADR-002-integration-decisions.md) §1) |

### 2. Сигналы и ордера

- Стратегия возвращает `TradeSignal`: **BUY**, **SELL**, **HOLD** (не boolean).
- Scheduler (`executeBots`) переводит сигнал в ордер через Terminal:
  - BUY → `POST /orders` type BUY, `orderType` MARKET (P0 default).
  - SELL → type SELL, MARKET.
- STOP/amend — через Terminal/Broker (US-OSE-001); стратегии P0 используют MARKET/LIMIT, STOP — для risk exits (P0 scope G1).

### 3. Триггер стратегии (P0)

- **Не** event bus / WebSocket candle close в P0.
- **`@Scheduled` cron**, интервал из `bot.candle-interval` (config), эмулирует закрытие свечи OsEngine.
- Lookback: `bot.candle-lookback` (min 26 для MACD); P0 — `CandleHistoryProvider` → PriceService; P1+ — MarketHistory с fallback на Price ([ADR-002](ADR-002-integration-decisions.md) §1).

### 4. Позиция и sizing

- Перед сигналом: `JournalClient.getPosition(userId, figi)`.
- Entry (BUY): только если нет позиции или qty ниже лимита; **pyramiding = false** по умолчанию.
- Exit (SELL): только если `quantity > 0`.
- `totalProfit` бота — из Journal (realized PnL aggregate) после round-trip.

### 5. Интеграция Journal ← Terminal

- При переходе ордера в **EXECUTED** Terminal вызывает **`JournalService`**: `POST /journal/fills` (идемпотентно по `orderId`, retry — [ADR-002](ADR-002-integration-decisions.md) §4).
- Owner wiring: **US-OSE-003** M3; US-OSE-001 — только lifecycle ордера в Terminal.
- Journal обновляет `Trade` + `Position`; не читает Broker напрямую в P0.

### 6. Backtest (P1) — shared module `fs-trading-core`

- **Amended by [ADR-003](ADR-003-strategy-shared-module.md)** (2026-06-17): Maven-модуль `fs-trading-core` (plain Java) — единые `TradingStrategy`, `TradeSignal`, `Candle` и impl для live и backtest.
- Пакет `com.fs.backtest` в **`BotOptimizationService`** — runtime backtest (`BacktestEngine`, `SimulatedJournal`), стратегии из **core**.
- Backtest использует `SimulatedJournal`, **не** Feign к live Terminal/Journal.

### 7. Security

- Bot → Terminal: S2S JWT (`X-Gateway-Internal-Jwt`) как в US-TE-001.
- Bot → Journal: тот же S2S JWT + `X-User-Id`.
- User → Journal: JWT пользователя через Gateway `/api/journal/**`.

## Consequences

**Positive**

- Чёткие границы сервисов; Journal масштабируется отдельно.
- P0 можно реализовать без WebSocket и без нового Backtest-модуля.

**Negative**

- Chatty Feign в scheduler (candles + position каждый tick) — mitigated: кэш lookback в Bot на 1 interval.
- Eventual consistency: fill в Journal после EXECUTED в Terminal — acceptable для P0 (retry + idempotency key).

**Risks**

- Дублирование strategy code live vs backtest — принято до P1; refactor при стабилизации API.

## Non-goals (P0)

- Screener / multi-figi bots
- User-supplied strategy scripts (Roslyn analog)
- Iceberg, auto-follow, copy trading
- Real-time candle WebSocket
- Отдельный `StrategyService`

## Success criteria

1. `TradingBotStrategyIntegrationTest` green: BUY then SELL по SMA сигналу.
2. E2E-02 (`e2e-bot-roundtrip.sh` → thin wrapper на `e2e-trading-flow.sh ose-02`, env `.env.test.ose`) проходит на `docker-compose.test.yml`.
3. Контракт `StrategyContext` не меняется breaking без нового ADR.

## References

- OsEngine: `BotPanel`, `BotTabSimple`, `Journal`
- FinancialSystem: `.cursor/plans/US-OSE-002-bot-strategy-runtime.plan.md`
- Test env: `.cursor/plans/US-TE-001-test-environment.plan.md`
