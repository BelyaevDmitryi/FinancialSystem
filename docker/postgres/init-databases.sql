-- Скрипт инициализации баз данных для Financial System
-- Этот скрипт выполняется автоматически при первом запуске PostgreSQL контейнера

-- Создание базы данных для User Service
CREATE DATABASE userdb;

-- Создание базы данных для Trading Terminal Service
CREATE DATABASE tradingdb;

-- Создание базы данных для Trading Bot Service
CREATE DATABASE botdb;

-- Создание базы данных для Journal Service (US-OSE-003, порт 8012)
CREATE DATABASE journaldb;

-- Создание базы данных для Market History Service (US-OSE-004, порт 8010)
CREATE DATABASE markethistorydb;

-- Предоставление всех привилегий пользователю postgres
GRANT ALL PRIVILEGES ON DATABASE userdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE tradingdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE botdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE journaldb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE markethistorydb TO postgres;
