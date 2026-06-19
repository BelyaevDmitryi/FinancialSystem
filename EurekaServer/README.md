# Eureka Server

Service Discovery сервер для микросервисной архитектуры финансовой системы.

## Порт

- **8761** — основной порт Eureka Server

## Веб-интерфейс

После запуска доступен мониторинг зарегистрированных сервисов:

- http://localhost:8761

## Модули Maven (корневой `pom.xml`)

12 Eureka-клиентов из корневого `pom.xml` (исключая `fs-trading-core` — shared jar, и `EurekaServer` — registry):

| Модуль | Eureka name | Порт |
|--------|-------------|------|
| UserService | `user-service` | 8001 |
| PriceService | `price-service` | 8003 |
| BrokerIntegrationService | `broker-integration-service` | 8004 |
| AnalyticsService | `analytics-service` | 8005 |
| TradingTerminalService | `trading-terminal-service` | 8006 |
| TradingBotService | `trading-bot-service` | 8007 |
| AdminPanelService | `admin-panel-service` | 8008 |
| DashboardService | `dashboard-service` | 8009 |
| MarketHistoryService | `market-history-service` | 8010 |
| BotOptimizationService | `bot-optimization-service` | 8011 |
| JournalService | `journal-service` | 8012 |
| ApiGateway | `api-gateway` | 8090 |

Порты — из `application.yml` каждого модуля.

## Использование Service Discovery

Сервисы обращаются друг к другу по имени, не по прямому URL:

- `lb://trading-terminal-service` вместо `http://localhost:8006`
- Spring Cloud LoadBalancer выбирает доступный экземпляр
- Поддерживается балансировка при нескольких репликах

## Health Checks

- Heartbeat каждые 30 секунд (`lease-renewal-interval-in-seconds`)
- Недоступные инстансы удаляются из реестра
- При восстановлении сервис регистрируется снова
