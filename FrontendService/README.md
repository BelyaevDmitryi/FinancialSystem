# Frontend Service

Веб-интерфейс для Financial System.

## Технологии

- React 18
- React Router DOM
- Material-UI (MUI)
- Recharts (для графиков)
- Axios (для HTTP запросов)
- Vite (сборщик)
- Vitest + Testing Library (smoke-тесты)

## Структура

```
FrontendService/
├── src/
│   ├── components/      # Переиспользуемые компоненты
│   ├── pages/           # Страницы приложения
│   ├── context/         # React Context (Auth)
│   ├── services/        # API клиент (api.js + domain helpers)
│   ├── test/            # Vitest setup
│   ├── App.jsx          # Главный компонент
│   └── main.jsx         # Точка входа
├── Dockerfile
├── nginx.conf           # Конфигурация Nginx
└── package.json
```

## Страницы

1. **Login/Register** — авторизация и регистрация
2. **Dashboard** — дашборд портфеля; pie «По позициям» / «По классам активов» (`GET /api/statistic/classes/{userId}`)
3. **Trading** — торговый терминал: создание ордеров, отмена PENDING (`POST /api/orders/{id}/cancel`)
4. **Analytics** — технический анализ (SMA, EMA)
5. **Bots** — управление торговыми ботами
6. **Backtest** — SMA backtest (`POST /api/backtest/run`), метрики + equity chart
7. **Optimizer** — grid-оптимизация (`POST /api/bot-optimization/grid`), top-K результатов
8. **Journal** — сделки и позиции (`GET /api/journal/trades`, `/positions`)
9. **Admin** — админ-панель со статистикой
10. **Profile** — профиль пользователя

## API helpers

| Модуль | Endpoints |
|--------|-----------|
| `services/api.js` | Базовый Axios + JWT refresh |
| `services/backtestApi.js` | `POST /api/backtest/run` |
| `services/optimizerApi.js` | `POST /api/bot-optimization/grid` |
| `services/journalApi.js` | `GET /api/journal/trades`, `/positions` |

Backtest и Optimizer по умолчанию используют FIGI `BBG004730N88` и диапазон дат `2026-01-01` … `2026-03-01` (как в `scripts/test-env/seed-candles.sh`).

## Запуск

### Локальная разработка

```bash
npm install
npm run dev
```

Приложение будет доступно на http://localhost:3000

Прокси Vite перенаправляет `/api/*` на ApiGateway (`http://localhost:8090`).

Для backtest/optimizer нужен P1 stack:

```bash
cp .env.test-p1.example .env.test-p1
docker compose -f docker-compose.test.yml -f docker-compose.test-p1.yml --env-file .env.test-p1 up -d
./scripts/test-env/seed-candles.sh
```

### Сборка и тесты

```bash
npm run build
npm test
```

### Docker

```bash
docker-compose up frontend-service
```

Приложение будет доступно на http://localhost:3000

## API

Frontend взаимодействует с API через ApiGateway (порт 8090).

Все запросы проксируются через Nginx в production режиме. Обработка 401 — refresh token через interceptor в `api.js`.
