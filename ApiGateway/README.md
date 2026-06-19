# API Gateway

Единая точка входа для всех микросервисов финансовой системы.

## Порт

- **8090** — основной порт API Gateway

## Аутентификация

Защищённые маршруты вызываются через Gateway с JWT:

```bash
Authorization: Bearer <token>
```

Gateway извлекает пользователя из токена и передаёт `X-User-Id` downstream-сервисам. Прямые вызовы сервисов (например, `:8007`) с ручным `X-User-Id` — только для отладки.

Получение токена:

```bash
POST http://localhost:8090/api/auth/signin
POST http://localhost:8090/api/auth/signup
```

## Маршрутизация

Маршруты из `application.yml` (Eureka `lb://`, `StripPrefix` указан для каждого пути).

| Gateway path | Сервис | StripPrefix |
|--------------|--------|-------------|
| `/api/auth/**` | `user-service` | 0 |
| `/api/users/**` | `user-service` | 1 |
| `/api/stocks/**` | `user-service` | 1 |
| `/api/statistic/**` | `user-service` | 1 |
| `/api/profile/**` | `user-service` | 0 |
| `/images/**` | `user-service` | 0 |
| `/api/price-service/**` | `price-service` | 1 |
| `/api/broker/**` | `broker-integration-service` | 1 |
| `/api/analytics/**` | `analytics-service` | 1 |
| `/api/orders/**` | `trading-terminal-service` | 1 |
| `/api/journal/**` | `journal-service` | 1 |
| `/api/bots/**` | `trading-bot-service` | 1 |
| `/api/market-history/**` | `market-history-service` | 1 |
| `/api/bot-optimization/**` | `bot-optimization-service` | 1 |
| `/api/backtest/**` | `bot-optimization-service` | 1 |
| `/api/admin/**` | `admin-panel-service` | 1 |
| `/api/dashboard/**` | `dashboard-service` | 1 |

При `StripPrefix=1` первый сегмент пути отбрасывается: `GET /api/orders` → `GET /orders` на `trading-terminal-service`.

Включён discovery locator (`lower-case-service-id: true`) для дополнительной маршрутизации по имени сервиса.

## Примеры через Gateway

### Пользователи и статистика

```bash
GET  http://localhost:8090/api/users/{id}
POST http://localhost:8090/api/users
GET  http://localhost:8090/api/statistic/classes/{userId}
```

### Ордера и боты (с JWT)

```bash
POST http://localhost:8090/api/orders
Authorization: Bearer <token>

POST http://localhost:8090/api/bots
Authorization: Bearer <token>
```

### Дашборд

```bash
GET http://localhost:8090/api/dashboard
Authorization: Bearer <token>
```

### Backtest и оптимизация

```bash
POST http://localhost:8090/api/backtest/run
POST http://localhost:8090/api/bot-optimization/grid
```

## CORS

Разрешены запросы с любого origin (`allowedOriginPatterns: *`), методы GET/POST/PUT/DELETE/OPTIONS.

## Health Check

```bash
GET http://localhost:8090/actuator/health
GET http://localhost:8090/actuator/gateway/routes
```
