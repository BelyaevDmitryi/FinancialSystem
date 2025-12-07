# API Gateway

Единая точка входа для всех микросервисов финансовой системы.

## Порт
- **8090** - основной порт API Gateway

## Маршрутизация

### User Service
- `/api/auth/**` → `user-service:8001`
- `/api/users/**` → `user-service:8001`
- `/api/stocks/**` → `user-service:8001` (stocks endpoints в UserService)
- `/api/statistic/**` → `user-service:8001`

### Stock Service
- `/api/stock-service/**` → `stock-service:8002`

### Price Service
- `/api/price-service/**` → `price-service:8003`

### Tinkoff Stock Service
- `/api/tinkoff/**` → `tinkoff-stock-service:8004`

## Примеры использования

### Аутентификация
```bash
POST http://localhost:8090/api/auth/signup
POST http://localhost:8090/api/auth/signin
```

### Работа с пользователями
```bash
GET http://localhost:8090/api/users/{id}
POST http://localhost:8090/api/users
PUT http://localhost:8090/api/users/{id}/stocks
DELETE http://localhost:8090/api/users/{id}
```

### Статистика
```bash
GET http://localhost:8090/api/statistic/classes/{userId}
GET http://localhost:8090/api/statistic/cost/{userId}
```

## CORS
API Gateway настроен для работы с CORS запросами из любого источника.

## Health Check
```bash
GET http://localhost:8090/actuator/health
```

