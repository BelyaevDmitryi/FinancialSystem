-- Добавление поля avatar_url в таблицу users
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
