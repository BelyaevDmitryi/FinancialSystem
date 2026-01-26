# Trading Terminal Service

Сервис торгового терминала для размещения и управления ордерами.

## Порт
- **8006** - основной порт Trading Terminal Service

## Функциональность

### Управление ордерами

1. **Создание ордера**
   - POST `/orders`
   - Требует заголовок `X-User-Id`
   - Поддерживает типы: BUY, SELL

2. **Получение ордеров пользователя**
   - GET `/orders`
   - GET `/orders/status/{status}` - фильтр по статусу

3. **Управление ордером**
   - GET `/orders/{orderId}` - получить ордер
   - POST `/orders/{orderId}/execute` - исполнить ордер
   - POST `/orders/{orderId}/cancel` - отменить ордер

## Статусы ордеров

- `PENDING` - ожидает исполнения
- `EXECUTED` - исполнен
- `CANCELLED` - отменен
- `REJECTED` - отклонен

## Примеры использования

### Создание ордера на покупку
```bash
POST http://localhost:8006/orders
X-User-Id: user123
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "type": "BUY",
  "quantity": 10,
  "price": 2500.50,
  "comment": "Покупка по техническому анализу"
}
```

### Получение всех ордеров
```bash
GET http://localhost:8006/orders
X-User-Id: user123
```

### Исполнение ордера
```bash
POST http://localhost:8006/orders/{orderId}/execute
```

## Health Check
```bash
GET http://localhost:8006/actuator/health
```
