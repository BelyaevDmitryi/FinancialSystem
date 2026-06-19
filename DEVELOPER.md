# Developer guide — FinancialSystem

Команды сборки и границы репозитория — в [AGENTS.md](AGENTS.md). Ниже только краткая шпаргалка для людей.

## Сборка

```bash
# Все модули
mvn -f "$(git rev-parse --show-toplevel)/pom.xml" clean verify

# Один сервис (пример)
mvn -f "$(git rev-parse --show-toplevel)/UserService/pom.xml" clean verify
```

Обёртки `mvnw` в репозитории **нет** — используйте `mvn` из PATH.

## Локальный запуск

См. [docker-compose.yml](docker-compose.yml) и [README.adoc](README.adoc). Порты — в `application.yml` каждого модуля.

## Агенты Cursor

- Реализация по плану: **@robot-coordinator** + файл в [`.cursor/plans/`](.cursor/plans/) (например, [US-TE-001](.cursor/plans/US-TE-001-test-environment.plan.md))
- Диаграммы: **@robot-architect**
- Ревью требований: **@robot-business-analyst**

Планы и skills настроены под **Spring Boot 3**; Quarkus/Micronaut в проекте не используются.

## Тестовая среда (без Tinkoff API)

Изолированная среда для проверки ордеров, ботов и стратегий без реального брокера.

### Быстрый старт

```bash
# Скопируйте файл с переменными окружения
cp .env.test.example .env.test

# Поднимите тестовый стек
docker compose -f docker-compose.test.yml --env-file .env.test up -d

# Проверьте статус сервисов
docker compose -f docker-compose.test.yml ps
```

### Порты тестовой среды

| Сервис | Порт | Описание |
|--------|------|----------|
| ApiGateway | 8090 | Единая точка входа для E2E |
| UserService | 8001 | Аутентификация (`POST /api/auth/signin`) |
| BrokerIntegrationService | 8004 | Mock-брокер (профиль `test`) |
| PriceService | 8003 | Цены инструментов |
| AnalyticsService | 8005 | SMA/EMA/MACD/волатильность |
| TradingTerminalService | 8006 | Ордера (`POST /api/orders`) |
| TradingBotService | 8007 | Торговые боты (`POST /api/bots`) |
| EurekaServer | 8761 | Service discovery |
| JournalService | 8012 | Журнал сделок/позиций (после US-OSE-003) |
| PostgreSQL | 5432 | БД (userdb, tradingdb, botdb, journaldb) |
| Redis | 6379 | Кэш цен |

### Health-check команды

```bash
# Проверить все сервисы разом
docker compose -f docker-compose.test.yml ps

# Отдельный сервис
curl -s http://localhost:8090/actuator/health | jq .
curl -s http://localhost:8001/actuator/health | jq .
```

### Ручная проверка через Gateway

```bash
# 1. Получить JWT токен
curl -X POST http://localhost:8090/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'

# 2. Создать ордер (подставьте токен)
curl -X POST http://localhost:8090/api/orders \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"figi":"BBG004730N88","type":"BUY","quantity":1,"price":100.00}'

# 3. Создать бота
curl -X POST http://localhost:8090/api/bots \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"figi":"BBG004730N88","strategy":"SMA","maxPositionSize":1000,"smaPeriod":20}'
```

Compose-файлы, `mvn verify` и E2E-матрица — в разделе [Тестирование](#тестирование).

### Остановка тестовой среды

```bash
docker compose -f docker-compose.test.yml down -v
```

## Тестирование

Сводная матрица проверок. Quick start, порты, health-check и ручные curl — в разделе [Тестовая среда](#тестовая-среда-без-tinkoff-api) выше.

### Compose-файлы

| Файл | Стек | Env |
|------|------|-----|
| [`docker-compose.test.yml`](docker-compose.test.yml) | P0 ([US-TE-001](.cursor/plans/US-TE-001-test-environment.plan.md)): gateway, сервисы TE, **journal-service**, Eureka, Postgres, Redis | `.env.test` (из `.env.test.example`) — minimal, **PENDING**; `.env.test.ose` (из `.env.test.ose.example`) — RM1, **EXECUTED** + Journal |
| [`docker-compose.test-p1.yml`](docker-compose.test-p1.yml) | extends `test.yml` + **market-history** + **bot-optimization** | [`.env.test-p1.example`](.env.test-p1.example) → `.env.test-p1` |

```bash
# P0 minimal (TE)
cp .env.test.example .env.test
docker compose -f docker-compose.test.yml --env-file .env.test up -d

# P0 OsEngine RM1
cp .env.test.ose.example .env.test.ose
docker compose -f docker-compose.test.yml --env-file .env.test.ose up -d

# P1 overlay ([ADR-002](docs/adr/ADR-002-integration-decisions.md))
cp .env.test-p1.example .env.test-p1
docker compose -f docker-compose.test.yml -f docker-compose.test-p1.yml --env-file .env.test-p1 up -d
```

Для `markethistorydb` на P1 нужен свежий volume Postgres (или `CREATE DATABASE markethistorydb` вручную на существующем кластере).

### Maven (`mvn verify`)

Обёртки `mvnw` в репозитории **нет** — используйте `mvn` из PATH. Границы репозитория — [AGENTS.md](AGENTS.md).

```bash
# Reactor (все модули)
mvn -f "$(git rev-parse --show-toplevel)/pom.xml" clean verify

# Один модуль (примеры)
mvn -f "$(git rev-parse --show-toplevel)/TradingTerminalService/pom.xml" clean verify
mvn -f "$(git rev-parse --show-toplevel)/TradingBotService/pom.xml" clean verify
```

Только локально, без тестов: `mvn ... clean package -DskipTests`.

### E2E-сценарии

Скрипт [`scripts/test-env/e2e-trading-flow.sh`](scripts/test-env/e2e-trading-flow.sh), переменная **`E2E_SCENARIO`** (или первый аргумент). E2E-02: [`e2e-bot-roundtrip.sh`](scripts/test-env/e2e-bot-roundtrip.sh) → `ose-02`.

После health gateway скрипт ждёт готовность auth (`E2E_AUTH_WAIT_MAX`, `E2E_AUTH_WAIT_INTERVAL`), затем signup/signin с retry на 5xx.

| `E2E_SCENARIO` | Env | Кратко |
|----------------|-----|--------|
| `te-smoke` (default) | `.env.test` | signin → bot → order **PENDING** |
| `ose-01` | `.env.test.ose` | MARKET → **EXECUTED** → journal (опц.) |
| `ose-02` | `.env.test.ose` | ACTIVE bot → scheduler → **EXECUTED** |
| `ose-05` | `.env.test.ose` | LIMIT amend / cancel |

```bash
./scripts/test-env/e2e-trading-flow.sh
E2E_SCENARIO=ose-01 ./scripts/test-env/e2e-trading-flow.sh
docker compose -f docker-compose.test.yml --env-file .env.test.ose up -d && ./scripts/test-env/e2e-bot-roundtrip.sh
```

P1 E2E (требуют `docker-compose.test-p1.yml`):

```bash
# Импорт свечей в markethistorydb (обязательно перед E2E-03 / E2E-04)
./scripts/test-env/seed-candles.sh

# E2E-03: POST /api/backtest/run (SMA backtest)
./scripts/test-env/e2e-backtest.sh
# E2E-04: POST /api/bot-optimization/grid (smaPeriod 10..20 step 2, totalRuns>=6)
./scripts/test-env/e2e-optimize.sh
```

`seed-candles.sh`: ждёт health gateway/market-history, `POST /api/market-history/import` для `BBG004730N88`, опрашивает статус до `COMPLETED`. Переменные: `FIGI`, `FROM`, `TO`, `USE_GATEWAY=false` для прямого `:8010`. Диапазон `FROM`/`TO` должен совпадать с телом запросов в `e2e-backtest.sh` и `e2e-optimize.sh` (по умолчанию `2026-01-01` … `2026-03-01`).

Подробная матрица: [`.cursor/plans/US-OSE-007-test-matrix.plan.md`](.cursor/plans/US-OSE-007-test-matrix.plan.md).

### Testcontainers

Интеграционные тесты с PostgreSQL в контейнере (`@ActiveProfiles("test")` в `TradingTerminalService`, `TradingBotService`, `JournalService`, `MarketHistoryService`) включаются флагом **`-Ddocker.tests=true`** (JUnit `@EnabledIfSystemProperty`); без флага они пропускаются, остальные тесты модуля выполняются. GitHub Actions (`.github/workflows/ci.yml`) запускает `mvn verify -Ddocker.tests=true`.
