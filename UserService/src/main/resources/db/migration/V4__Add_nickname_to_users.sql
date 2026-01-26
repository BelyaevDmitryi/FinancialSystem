-- Добавление поля nickname в таблицу users
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(255);

-- Заполняем nickname значением из name для существующих пользователей
UPDATE users SET nickname = name WHERE nickname IS NULL;
