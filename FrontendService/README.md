# Frontend Service

Веб-интерфейс для Financial System.

## Технологии

- React 18
- React Router DOM
- Material-UI (MUI)
- Recharts (для графиков)
- Axios (для HTTP запросов)
- Vite (сборщик)

## Структура

```
FrontendService/
├── src/
│   ├── components/      # Переиспользуемые компоненты
│   ├── pages/           # Страницы приложения
│   ├── context/         # React Context (Auth)
│   ├── services/        # API клиент
│   ├── App.jsx          # Главный компонент
│   └── main.jsx         # Точка входа
├── Dockerfile
├── nginx.conf           # Конфигурация Nginx
└── package.json
```

## Страницы

1. **Login/Register** - Авторизация и регистрация
2. **Dashboard** - Дашборд с портфелем и графиками
3. **Trading** - Торговый терминал для создания ордеров
4. **Analytics** - Технический анализ (SMA, EMA)
5. **Bots** - Управление торговыми ботами
6. **Admin** - Админ панель со статистикой

## Запуск

### Локальная разработка

```bash
npm install
npm run dev
```

Приложение будет доступно на http://localhost:3000

### Docker

```bash
docker-compose up frontend-service
```

Приложение будет доступно на http://localhost:3000

## API

Frontend взаимодействует с API через ApiGateway (порт 8090).

Все запросы проксируются через Nginx в production режиме.
