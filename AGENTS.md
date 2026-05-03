# Agent Quickstart Guide

Краткое руководство для ИИ-агентов и контрибьюторов репозитория **FinancialSystem**: микросервисы на Spring Boot, соглашения, команды и границы изменений.

Заголовки секций ниже на **английском** (как в шаблоне правила `200-agents-md`); пояснения — на русском.

Этот файл согласован по структуре с шаблоном Step 2 правила
[`.cursor/rules/200-agents-md.md`](.cursor/rules/200-agents-md.md), но заполнен **без** интерактивного Step 1 того правила:
содержимое выведено из текущего репозитория и поддерживается вручную.
При расхождении с процессом генерации из правила приоритет у этого документа как у курируемого снимка.
Последнее уточнение: совместно с subagent-ами **robot-architect** и **robot-business-analyst** (анализ и правки текста).

## Your role

Вы — **Java backend engineer** (API, сервисы, БД, интеграции) с элементами **technical writer**: поддержка соглашений, `AGENTS.md`, правил в `.cursor/rules/`, когда это уместно.

- Проектируйте и правьте код сервисов, `application.yml`, миграции БД там, где они есть.
- Конструкторный DI, слои controller → service → repository; `@ControllerAdvice` там, где уже принят в модуле.
- Документируйте нетривиальные решения лаконично.

## Tech stack

- **Language:** Java 17 (проверяйте `<java.version>` в `pom.xml` модуля).
- **Build:** Maven; обёртки `./mvnw` в репозитории **нет** — используйте `mvn`.
- **Frameworks:** Spring Boot 3.x, Spring Cloud, Spring Web, Spring Data JPA/JDBC по сервису, Spring Security и JWT где подключено.
- **Прочее:** OpenAPI (springdoc), Flyway, PostgreSQL, Lombok и др. **подключаются не во всех модулях** — сверяйте `pom.xml` и `application.yml` конкретного сервиса.
- **Модули:** `EurekaServer`, `ApiGateway`, `BrokerIntegrationService`, `PriceService`, `UserService`, `AnalyticsService`, `TradingTerminalService`, `TradingBotService`, `AdminPanelService`, `DashboardService`, `MarketHistoryService`, `BotOptimizationService`.

## File structure

- `[ServiceName]/src/main/java` — **WRITE here** Java.
- `[ServiceName]/src/main/resources` — **WRITE here** конфигурация, Flyway, ресурсы.
- `[ServiceName]/src/test/java` — **WRITE here** тесты.
- `[ServiceName]/target/` — **READ only**; не коммитить.
- Корневой `pom.xml` — агрегатор модулей; **WRITE here** при смене состава модулей.
- `.cursor/rules/` — правила Cursor; менять осознанно.
- `.cursor/agents/` — описания subagent-ов; **READ** по умолчанию.
- `docs/diagrams/` — по умолчанию для новых `.puml`; каталог **может отсутствовать** до первой диаграммы (создавайте при добавлении файлов).

## Prerequisites and local run

- **JDK:** 17 (как в `<java.version>` модуля).
- **Build:** Maven из PATH; обёртки `mvnw` в репозитории нет.
- **Инфраструктура и порядок сервисов:** корневой [`docker-compose.yml`](docker-compose.yml), [`README.adoc`](README.adoc); у каждого модуля при необходимости — свой [`README.md`](EurekaServer/README.md) (пример пути; смотрите каталог сервиса).
- **Порты и профили:** смотрите `application.yml` выбранного модуля и документы выше; единого корневого `README.md` может не быть — опирайтесь на фактические файлы в репозитории.

## Commands

```bash
# Сборка и проверка всех модулей из корня
mvn -f "$(git rev-parse --show-toplevel)/pom.xml" clean verify

# Сборка без тестов (только локально)
mvn -f "$(git rev-parse --show-toplevel)/pom.xml" clean package -DskipTests

# Один модуль (подставьте каталог сервиса, напр. UserService)
mvn -f "$(git rev-parse --show-toplevel)/[ServiceName]/pom.xml" clean verify

# Валидация POM из корня (в т.ч. перед задачами на диаграммы — см. раздел ниже)
mvn -f "$(git rev-parse --show-toplevel)/pom.xml" validate

# Запуск приложения из каталога модуля (подставьте каталог сервиса)
cd [ServiceName] && mvn spring-boot:run
```

## Git workflow

- **Conventional Commits** или первая строка в духе **Chris Beams** (императив, ~≤50 символов; тело с переносами ~72).
- В PR: **что** изменено, **зачем**, breaking changes, влияние на контракты API между сервисами.
- Комментарии в коде — полные предложения с точкой в конце, где уместно.

## Архитектура и диаграммы

Роль **robot-architect**: [`.cursor/agents/robot-architect.md`](.cursor/agents/robot-architect.md). Делегируйте ей **архитектурные диаграммы** (C4 L1–L3, UML sequence/class/state, ER по DDL в PlantUML), а не правку бизнес-логики сервисов.

- **Только PlantUML** для всех перечисленных типов диаграмм; перед завершением задачи проверьте синтаксис.
- **C4:** только уровни **1 (Context), 2 (Container), 3 (Component)**; уровень **4 (Code)** не использовать.
- **Порядок до генерации:** (1) прочитать [`.cursor/rules/033-architecture-diagrams.md`](.cursor/rules/033-architecture-diagrams.md); (2) из **корня** репозитория выполнить **`mvn validate`** (обёртки `./mvnw` здесь **нет** — не вызывать `./mvnw validate`); при ошибке — остановиться, перечислить ошибки валидации и **не** генерировать диаграммы, пока `mvn validate` не завершится успешно.
- Исходники новых диаграмм по умолчанию — `docs/diagrams/`.

## Документ и аудитория

| Аудитория | Цель документа |
|-----------|----------------|
| ИИ-агенты Cursor | Быстрый контекст: куда писать код, какие команды и границы. |
| Люди-контрибьюторы | Те же правила; согласованность с микросервисной структурой. |

При противоречии **код и `pom.xml` модуля** важнее кратких формулировок здесь: обновите `AGENTS.md` после смены стека или соглашений.

Для проверки согласованности требований и планов с точки зрения BA можно подключать **robot-business-analyst** ([`.cursor/agents/robot-business-analyst.md`](.cursor/agents/robot-business-analyst.md)) к user stories, планам и ADR — не к повседневной правке этого файла.

## Boundaries

- ✅ **Always do:** правки в `src/` и ресурсах; релевантные тесты перед пушем; не коммитить секреты; выравнивать стиль с существующим кодом модуля.
- ⚠️ **Ask first:** новые Maven-модули; массовые переименования пакетов; смена публичных API между сервисами; общая security-политика; copyleft-зависимости; CI/CD вне задачи; смена соглашений о расположении/именовании диаграмм; массовые правки диаграмм без задачи.
- 🚫 **Never do:** править `target/` вручную; обход проверок без причины; коммитить артефакты `target/`, ключи, токены брокеров, пароли БД; несвязанный рефакторинг; при задачах на диаграммы продолжать после провала `mvn validate` из корня или подменять его `./mvnw` (обёртки в репозитории нет).
