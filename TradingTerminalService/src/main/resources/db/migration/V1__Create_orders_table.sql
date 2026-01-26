-- Создание таблицы ордеров
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    figi VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    quantity NUMERIC(19, 2) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    executed_at TIMESTAMP,
    comment TEXT
);

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_figi ON orders(figi);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
