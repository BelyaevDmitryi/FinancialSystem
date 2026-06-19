# Broker Integration Service

Сервис интеграции с брокерами через паттерн Adapter.

## Порт

- **8004** — основной порт Broker Integration Service

## Реализованные адаптеры

**Реализовано:** TINKOFF, MOCK. **Планируется:** VTB, SBER и другие брокеры с торговлей.

| Брокер | Класс | Профиль | Статус |
|--------|-------|---------|--------|
| **TINKOFF** | `TinkoffBrokerAdapter` | default (`!test & !mock-broker`) | MARKET/LIMIT/STOP, amend LIMIT, cancel |
| **MOEX_ISS** | `MoexIssDataAdapter` | default (`!mock-broker`) | Data-only (котировки, инструменты) |
| **MOCK** | `MockBrokerAdapter` | `test`, `mock-broker` | Реализовано |

**Планируется:** VTB, SBER и другие брокеры с торговлей (интерфейс `BrokerAdapter` + регистрация в `BrokerFactory`).

В тестовой среде (`spring.profiles.active=test`) активен `MockBrokerAdapter`; `TBANK_TOKEN` не требуется.

## Fallback Tinkoff → MOEX ISS

Для `GET /broker/stocks/{ticker}` (и вызова без явного `broker`, либо с `broker=TINKOFF`):

1. Запрос в Tinkoff Invest API.
2. При `404` / `StockNotFoundException` — повтор через MOEX ISS (только чтение).

Пример (через Gateway):

```bash
curl -s "http://localhost:8090/api/broker/stocks/MOEXONLY?broker=TINKOFF"
```

Для цен по FIGI fallback выполняет **PriceService** (Tinkoff → MOEX по тикеру, synthetic FIGI `MOEX:{TICKER}`).

## Архитектура

- **BrokerAdapter** — интерфейс адаптера
- **TinkoffBrokerAdapter** — Т-Инвестиции (Tinkoff Invest API)
- **MoexIssDataAdapter** — MOEX ISS (read-only: инструменты, last price)
- **MockBrokerAdapter** — синтетические котировки и ордера для E2E/тестов
- **BrokerFactory** — выбор адаптера по имени и цепочка fallback для `getStock(ticker)`

## API

Через Gateway: `http://localhost:8090/api/broker/...`  
Прямой вызов: `http://localhost:8004/broker/...`

### Получить информацию об акции

```
GET /broker/stocks/{ticker}?broker=TINKOFF
```

При отсутствии в Tinkoff сервис автоматически запрашивает MOEX ISS.

### Получить информацию об акциях по тикерам

```
POST /broker/stocks/getStocksByTickers
Content-Type: application/json

{
  "tickers": ["SBER", "GAZP"]
}
```

### Получить цены по FIGI

```
POST /broker/prices
Content-Type: application/json

{
  "figies": ["BBG004730N88", "MOEX:SBER"]
}
```

Для MOEX ISS используйте synthetic FIGI `MOEX:{TICKER}` и параметр `?broker=MOEX_ISS`.

### Список доступных брокеров

```
GET /broker/available
```

## Конфигурация

По умолчанию — TINKOFF (`application.yml`):

```yaml
broker:
  default: TINKOFF
  moex:
    base-url: https://iss.moex.com
    board: TQBR
```

Токен Tinkoff: переменная `TBANK_TOKEN`. MOEX ISS — публичный API, ключ не требуется.

## Health Check

```bash
GET http://localhost:8004/actuator/health
```

## Тесты

- `MoexIssDataAdapterTest` — WireMock MOEX ISS (без сети в CI)
- `BrokerFactoryFallbackTest` — цепочка Tinkoff → MOEX для тикера
- `TinkoffBrokerAdapterTest` — amend LIMIT (`replaceOrder`), cancel STOP/exchange (mock SDK)
