# Market History Service

Хранение и импорт исторических свечей.

## Порт

- **8010** — основной порт Market History Service

## API

Через Gateway: `http://localhost:8090/api/market-history/...`  
Прямой вызов: `http://localhost:8010/market-history/...`

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/market-history/candles` | Свечи по FIGI (`figi`, `from`, `to`, `interval`, `broker`) |
| POST | `/market-history/import` | Асинхронный импорт свечей из брокера в БД |
| GET | `/market-history/import/{jobId}` | Статус задачи импорта |

## Примеры через Gateway

```bash
GET "http://localhost:8090/api/market-history/candles?figi=BBG004730N88&from=2026-01-01T00:00:00Z&to=2026-03-01T00:00:00Z&interval=DAY"

POST http://localhost:8090/api/market-history/import
Content-Type: application/json

{"figi":"BBG004730N88","from":"2026-01-01T00:00:00Z","to":"2026-03-01T00:00:00Z","interval":"DAY"}
```

## Health Check

```bash
GET http://localhost:8010/actuator/health
```
