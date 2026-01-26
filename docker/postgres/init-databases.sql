-- Скрипт инициализации баз данных для Financial System
-- Этот скрипт выполняется автоматически при первом запуске PostgreSQL контейнера

-- Создание базы данных для User Service
CREATE DATABASE userdb;

-- Создание базы данных для Trading Terminal Service
CREATE DATABASE tradingdb;

-- Создание базы данных для Trading Bot Service
CREATE DATABASE botdb;

-- Предоставление всех привилегий пользователю postgres
GRANT ALL PRIVILEGES ON DATABASE userdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE tradingdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE botdb TO postgres;
