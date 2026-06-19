# Journal Service

Журнал сделок и позиций пользователя.

## Порт

- **8012** — основной порт Journal Service

## API

Через Gateway: `http://localhost:8090/api/journal/...`  
Прямой вызов: `http://localhost:8012/journal/...`

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/journal/trades` | Сделки пользователя |
| GET | `/journal/positions` | Все позиции |
| GET | `/journal/positions/{figi}` | Позиция по FIGI |
| POST | `/journal/fills` | Запись fill (S2S, роль `INTERNAL`) |

Пользовательские endpoints требуют `X-User-Id` (через Gateway — из JWT).

## Пример через Gateway

```bash
GET http://localhost:8090/api/journal/trades
Authorization: Bearer <token>
```

## Health Check

```bash
GET http://localhost:8012/actuator/health
```
