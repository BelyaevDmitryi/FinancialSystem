# Bot Optimization Service

Backtest и grid-оптимизация параметров стратегий на исторических свечах.

## Порт

- **8011** — основной порт Bot Optimization Service

## API

Через Gateway:

| Метод | Gateway path | Описание |
|-------|--------------|----------|
| POST | `/api/backtest/run` | Backtest SMA на свечах из Market History |
| POST | `/api/bot-optimization/sma-trend-grid` | Перебор периода SMA |
| POST | `/api/bot-optimization/grid` | Многопараметрический grid с фильтрами |

Прямой вызов (без префикса `/api`):

- `POST http://localhost:8011/backtest/run`
- `POST http://localhost:8011/bot-optimization/grid`

## Примеры через Gateway

```bash
POST http://localhost:8090/api/backtest/run
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-03-01T00:00:00Z",
  "smaPeriod": 20
}

POST http://localhost:8090/api/bot-optimization/grid
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-03-01T00:00:00Z",
  "parameters": [...]
}
```

## Health Check

```bash
GET http://localhost:8011/actuator/health
```
