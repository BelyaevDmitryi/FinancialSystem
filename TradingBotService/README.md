# Trading Bot Service

Сервис автоматической торговли на основе технического анализа.

## Порт

- **8007** — основной порт Trading Bot Service

## Функциональность

### Управление ботами

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/bots` | Создать бота |
| GET | `/bots` | Список ботов пользователя |
| PUT | `/bots/{botId}/status` | Изменить статус (`?status=ACTIVE\|PAUSED\|...`) |
| DELETE | `/bots/{botId}` | Удалить бота |
| GET | `/bots/admin/stats/bots` | Статистика ботов (админ) |

Идентификатор пользователя — заголовок `X-User-Id` (Gateway выставляет из JWT).

### Стратегии

- **MACD_CROSSOVER** — пересечение MACD и сигнальной линии
- **SMA_CROSSOVER** — цена выше SMA
- **VOLATILITY_BREAKOUT** — пробой по волатильности
- **EMA_TREND** — восходящий тренд по EMA

### Планировщик

Боты выполняются по расписанию (`bot.scheduler.fixed-delay`, по умолчанию каждую минуту). Scheduler использует S2S JWT (`X-Gateway-Internal-Jwt`) при вызовах Trading Terminal.

## Примеры через API Gateway

### Создание бота

```bash
POST http://localhost:8090/api/bots
Authorization: Bearer <token>
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

### Изменение статуса

```bash
PUT http://localhost:8090/api/bots/{botId}/status?status=PAUSED
Authorization: Bearer <token>
```

### Удаление бота

```bash
DELETE http://localhost:8090/api/bots/{botId}
Authorization: Bearer <token>
```

## Health Check

```bash
GET http://localhost:8007/actuator/health
```
