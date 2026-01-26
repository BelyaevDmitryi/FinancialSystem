# Broker Integration Service

Сервис интеграции с различными брокерами (Т-Инвестиции, ВТБ-Инвестиции, Сбер-Инвестиции, MOEX и т.д.).

## Порт
- **8004** - основной порт Broker Integration Service

## Архитектура

Сервис использует паттерн Adapter для поддержки различных брокеров:

- **BrokerAdapter** - интерфейс для адаптеров брокеров
- **TinkoffBrokerAdapter** - реализация для Т-Инвестиций
- **BrokerFactory** - фабрика для выбора подходящего брокера

## Добавление нового брокера

Для добавления нового брокера (например, ВТБ-Инвестиции):

1. Создать класс, реализующий `BrokerAdapter`:
```java
@Component("vtbBrokerAdapter")
public class VtbBrokerAdapter implements BrokerAdapter {
    // Реализация методов
}
```

2. Spring автоматически обнаружит адаптер через `BrokerFactory`

3. Использовать в запросах: `?broker=VTB`

## API

### Получить информацию об акции
```
GET /broker/stocks/{ticker}?broker=TINKOFF
```

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
  "figies": ["BBG004730N88", "BBG004730ZJ9"]
}
```

### Получить список доступных брокеров
```
GET /broker/available
```

## Конфигурация

По умолчанию используется Tinkoff. Можно изменить в `application.yml`:
```yaml
broker:
  default: TINKOFF  # или VTB, SBER, MOEX и т.д.
```

## Health Check
```bash
GET http://localhost:8004/actuator/health
```
