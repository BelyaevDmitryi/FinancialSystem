# ADR-002: Integration Decisions (BA Open Questions)

**Status:** Accepted  
**Date:** 2026-05-23  
**Deciders:** Architecture (post BA cross-plan review)  
**Related:** [ADR-001](ADR-001-strategy-runtime-p0.md), EPIC-OSE-000, US-OSE-001…007, US-TE-001  
**OsEngine reference:** `/Users/belyaev.dmitryi/CursorProjects/OsEngine` — `BotTabSimple` + `ConnectorCandles` (live), `OsDataSet` / `OsDataMaster` (persisted history), `Journal` (fills → positions)

## Context

BA-аудит выявил открытые вопросы: источник свечей P0, состав `docker-compose.test-p1.yml`, Dashboard в compose, граница US-OSE-001 vs Journal ingest. Требования: **гибкость**, **без потери данных** (идемпотентность + retry), **паритет с OsEngine**.

## Decision

### 1. Источник свечей (P0 vs P1) — гибкий dual-path

| Фаза | OsEngine аналог | FinancialSystem |
|------|-----------------|-----------------|
| **P0 live (RM1)** | `BotTabSimple` → `ConnectorCandles.Candles()` — поток с коннектора | **`PriceService`** (lookback / last prices) + **`AnalyticsService`** для индикаторов |
| **P1+ (RM2)** | `OsDataSet.GetCandlesAllHistory()` — персистентная история | **`MarketHistoryService`** DB-first; при отсутствии данных — fallback на broker/Price |

**Контракт в `TradingBotService`:** интерфейс `CandleHistoryProvider` с реализациями:

- `PriceServiceCandleProvider` — P0 default (совместим с TE-001 и текущим `BotStrategyExecutor`).
- `MarketHistoryCandleProvider` — P1 (Feign к `MarketHistoryService`).
- `DelegatingCandleHistoryProvider` — порядок: MarketHistory → Price (не ломает RM1 compose без MarketHistory).

Триггер стратегии без изменений (ADR-001 §3): `@Scheduled` cron эмулирует `CandleFinishedEvent`, не WebSocket.

**Потери данных:** при недоступности MarketHistory в P1+ бот **не** пропускает тик молча — fallback на Price + WARN в лог; при пустом lookback (< min для стратегии) — HOLD, без ордера.

### 2. `docker-compose.test-p1.yml` — extends P0

```yaml
# Концепт (фактический файл — US-OSE-007 task 8)
services:
  # наследуются из docker-compose.test.yml через compose extends / include
```

- **База:** `docker-compose.test.yml` (полный P0: eureka, postgres, redis, user, broker-mock, **price**, **analytics**, terminal, bot, gateway, **journal-service** после US-OSE-003).
- **P1 overlay:** `docker-compose.test-p1.yml` **extends** base + `market-history-service`, `bot-optimization-service`; env: `.env.test-p1.example` (merge с `.env.test`).
- E2E-03/04 выполняются на **полном** стеке (не delta-only).

### 3. Dashboard в P0 — вне compose

- **Не** добавлять `dashboard-service` в `docker-compose.test.yml` (как TE-001).
- Проверка интеграции Journal ↔ Dashboard: **MockMvc / slice** + mock `JournalClient` (US-OSE-007 task 4).
- AC3b US-OSE-003: optional — не блокирует RM1.

### 4. Fill pipeline: владелец и надёжность

| Ответственность | План |
|-----------------|------|
| Ордер → EXECUTED в Terminal DB + broker sync | **US-OSE-001** (M2) |
| Publish fill → Journal | **US-OSE-003** (M3) — единственный owner |
| Идемпотентный ingest, позиции, PnL | **JournalService** |

**US-OSE-001 M2 DoD** не включает вызов Journal (ссылка на ADR-002 §4).

**Надёжность (без потери fills):**

1. Terminal после **commit** статуса EXECUTED вызывает `POST /journal/fills` (Feign).
2. **Идемпотентность:** уникальный ключ `orderId` (+ `fillSequence` если partial fills в P2); повторный POST → `200` без дубля `Trade`/`Position`.
3. **Retry:** Spring Retry (или Feign retry) на 5xx/timeout: 3 попытки, exponential backoff; после исчерпания — ERROR log + метрика `journal.fill.publish.failed` (ордер в Terminal остаётся EXECUTED; восстановление — ручной replay или будущий outbox P2).
4. Заголовки S2S: `X-Gateway-Internal-Jwt` + `X-User-Id` + correlation `orderId` (ADR-001 §7).

OsEngine: Journal обновляется из исполнений коннектора in-process; в микросервисах — **at-least-once publish + idempotent consume**.

### 5. US-OSE-002 и JournalClient

| Milestone | JournalClient |
|-----------|---------------|
| M1 | Mockito / stub в unit-тестах `DefaultStrategyContext` |
| M3 | Реальный Feign после US-OSE-003 M3 (ingest + GET positions) |

`blocked-by` US-OSE-002: US-OSE-001 M2, US-OSE-003 **M2** (read API), полный E2E — **003 M3**.

### 6. Backtest API path (minor)

Единый путь: `POST /api/backtest/run` на Gateway → `bot-optimization-service` (внутренний controller `/backtest/run`).

## Consequences

- RM1 E2E-02 остаётся на `test.yml` без MarketHistory.
- RM2 E2E-03/04 — на `test-p1.yml` с полным стеком.
- Планы US-OSE-001/002/003/007, EPIC, TE-001, ADR-001 §1/§3 синхронизированы с этим ADR.

## References

- OsEngine: `project/OsEngine/OsTrader/Panels/Tab/BotTabSimple.cs`, `project/OsEngine/OsData/OsDataSet.cs`
- ADR-001, `.cursor/plans/EPIC-OSE-000-osengine-parity-roadmap.plan.md`
