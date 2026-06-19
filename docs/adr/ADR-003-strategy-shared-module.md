# ADR-003: Shared Strategy Module (fs-trading-core)

**Status:** Accepted  
**Date:** 2026-06-17  
**Deciders:** Architecture (US-OSE-008)  
**Supersedes:** [ADR-001](ADR-001-strategy-runtime-p0.md) §6 (дублирование live/backtest)  
**Related:** US-OSE-008, EPIC-OSE-000, OsEngine §12.2

## Context

В P0/P1 live-стратегии (`TradingBotService`) вызывали `AnalyticsService` через Feign на каждом scheduler tick, а backtest (`BotOptimizationService`) дублировал логику SMA локально в `SmaBacktestStrategy`. Golden path live ↔ backtest не гарантирован.

OsEngine выполняет индикаторы **in-process** в том же процессе, что и стратегия. Для паритета FS стратегия должна считать индикаторы локально в shared-модуле.

## Decision

### 1. Maven-модуль `fs-trading-core`

- Plain Java 17 jar, **без Spring**.
- Пакет `com.fs.trading.core`: `Candle`, `TradeSignal`, `TradingStrategy`, реализации стратегий, `TechnicalIndicators`.
- Подключён в корневой `pom.xml`; зависимости: `TradingBotService`, `BotOptimizationService`.

### 2. Единая реализация стратегий

| Стратегия | Класс в core | Live adapter | Backtest |
|-----------|--------------|--------------|----------|
| SMA | `SmaCrossoverStrategy` | `TradingBotService` wrapper | `BacktestEngine` |
| MACD | `MacdCrossoverStrategy` | wrapper | (P1+ при необходимости) |
| EMA | `EmaTrendStrategy` | wrapper | (P1+ при необходимости) |
| Volatility | `VolatilityBreakoutStrategy` | wrapper | (P1+ при необходимости) |

Контракт `TradingStrategy` в core:

```java
TradeSignal evaluate(List<Candle> candlesUpToBar, BigDecimal positionQty, int period);
```

`period` — `smaPeriod` / `emaPeriod`; для MACD и Volatility игнорируется (фиксированные окна в impl).

### 3. AnalyticsService — вне hot path

- `AnalyticsService` **остаётся** для REST/UI (`AnalyticsPage`, графики серий SMA/EMA/MACD).
- Scheduler бота (`executeBots`) **не** вызывает Feign к Analytics для расчёта сигналов.
- Допустимо расхождение precision UI vs core на уровне серий для графика; сигналы live/backtest — из core.

### 4. Runtime API (ADR-001 §1) — уточнение

| Компонент | Расположение |
|-----------|--------------|
| `TradeSignal`, `Candle`, `TradingStrategy`, strategy impl | **`fs-trading-core`** |
| `BotStrategyExecutor`, `StrategyContext` | `TradingBotService` |
| `BacktestEngine`, `SimulatedJournal` | `BotOptimizationService` |

## Consequences

**Positive**

- Детерминированный golden path: `GoldenSmaBacktestTest` и `SmaCrossoverStrategyTest` (core) на одном CSV fixture.
- Меньше chatty Feign в scheduler (candles + position остаются).
- OsEngine parity: in-process indicator math.

**Negative**

- Дублирование формул core vs AnalyticsService для UI — mitigated: общие тесты на fixture, рефактор Analytics на core — отдельная задача.

**Risks**

- Расхождение UI-графика и live-сигнала при изменении только Analytics — документировано; изменения индикаторов требуют правки core + тестов.

## Success criteria

1. `mvn -pl fs-trading-core,BotOptimizationService,TradingBotService clean verify` green.
2. `GoldenSmaBacktestTest` и core `SmaCrossoverStrategyTest` согласованы на `golden-sma.csv`.
3. `TradingBotStrategyIntegrationTest` проходит без mock Feign SMA.

### 5. Paper trading (US-OSE-009)

| Компонент | Поведение |
|-----------|-----------|
| `TradingBot.paper` | Флаг на боте; default `true` (`fs.bot.default-paper`). Scheduler передаёт `paper` в `POST /orders`. |
| `Order.paper` | Колонка в Terminal DB; `CreateOrderDto.paper=true` → **без** Feign к `BrokerIntegrationService`. |
| `PaperFillSimulator` | Last price из `PriceService` → `EXECUTED` + `JournalFillPublisher`. |
| Live path | `paper=false` — без изменений (broker Feign, RM1 mock/real). |

Paper routing **в Terminal**, не отдельный `PaperBrokerAdapter` — меньше surface между сервисами.

## References

- [ADR-001 §6 (amended)](ADR-001-strategy-runtime-p0.md)
- `.cursor/plans/US-OSE-008-unified-strategy-live-backtest.plan.md`
- `.cursor/plans/US-OSE-009-paper-trading-mode.plan.md`
