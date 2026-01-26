# Analytics Service

Сервис для технического анализа финансовых инструментов.

## Порт
- **8005** - основной порт Analytics Service

## Функциональность

### Технические индикаторы

1. **SMA (Simple Moving Average)** - Простая скользящая средняя
   - POST `/analytics/sma`
   - Требует минимум N точек данных, где N - период

2. **EMA (Exponential Moving Average)** - Экспоненциальная скользящая средняя
   - POST `/analytics/ema`
   - Требует минимум N точек данных, где N - период

3. **Volatility** - Волатильность (стандартное отклонение)
   - POST `/analytics/volatility`
   - Возвращает волатильность в процентах и стандартное отклонение

4. **MACD (Moving Average Convergence Divergence)**
   - POST `/analytics/macd`
   - Требует минимум 26 точек данных
   - Возвращает MACD, сигнальную линию и гистограмму

## Примеры использования

### Расчет SMA
```bash
POST http://localhost:8005/analytics/sma
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "period": 20,
  "priceData": [
    {"figi": "BBG004730N88", "price": 100.50, "timestamp": "2024-01-01T10:00:00"},
    ...
  ]
}
```

### Расчет MACD
```bash
POST http://localhost:8005/analytics/macd
Content-Type: application/json

{
  "figi": "BBG004730N88",
  "priceData": [...]
}
```

## Health Check
```bash
GET http://localhost:8005/actuator/health
```
