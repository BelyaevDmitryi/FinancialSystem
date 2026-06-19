# Trading Terminal Service

Сервис торгового терминала: размещение и управление ордерами.

## Порт

- **8006** — основной порт Trading Terminal Service

## Функциональность

### Управление ордерами

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/orders` | Создать ордер |
| GET | `/orders` | Все ордера пользователя |
| GET | `/orders/status/{status}` | Фильтр по статусу |
| GET | `/orders/{orderId}` | Ордер по ID |
| PATCH | `/orders/{orderId}` | Изменить параметры (amend, LIMIT PENDING) |
| POST | `/orders/{orderId}/execute` | Исполнить ордер |
| POST | `/orders/{orderId}/cancel` | Отменить ордер |
| GET | `/orders/stats` | Статистика пользователя |
| GET | `/orders/admin/stats/orders` | Глобальная статистика (админ) |

Идентификатор пользователя — `X-User-Id` (через Gateway из JWT).

### Типы ордеров

- **Направление:** `BUY`, `SELL` (`OrderType`)
- **Исполнение у брокера:** `MARKET`, `LIMIT`, `STOP` (`BrokerOrderType`, по умолчанию `LIMIT`)
- **STOP:** поле `stopPrice` — цена активации стоп-заявки

### Amend (изменение LIMIT)

`PATCH /orders/{orderId}` с телом `{"price": <новая цена>}` — только для ордеров в статусе `PENDING` с типом `LIMIT`.

### Статусы

- `PENDING` — ожидает исполнения
- `EXECUTED` — исполнен
- `CANCELLED` — отменён
- `REJECTED` — отклонён

## Примеры через API Gateway

### LIMIT-ордер на покупку

```bash
POST http://localhost:8090/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "type": "BUY",
  "quantity": 10,
  "price": 2500.50,
  "orderType": "LIMIT",
  "comment": "Покупка по лимиту"
}
```

### MARKET-ордер

```bash
POST http://localhost:8090/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "type": "BUY",
  "quantity": 1,
  "price": 100.00,
  "orderType": "MARKET"
}
```

### STOP-ордер

```bash
POST http://localhost:8090/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "type": "SELL",
  "quantity": 5,
  "price": 95.00,
  "orderType": "STOP",
  "stopPrice": 98.00
}
```

### Amend LIMIT

```bash
PATCH http://localhost:8090/api/orders/{orderId}
Authorization: Bearer <token>
Content-Type: application/json

{"price": 2400.00}
```

### Отмена

Только для ордеров в статусе `PENDING`. При `paper=true` — локальная отмена без вызова брокера. При live-ордере с `brokerOrderId` — сначала отмена у брокера; при ошибке брокера статус **не** меняется (HTTP 4xx/5xx).

```bash
POST http://localhost:8090/api/orders/{orderId}/cancel
Authorization: Bearer <token>
```

## Health Check

```bash
GET http://localhost:8006/actuator/health
```
