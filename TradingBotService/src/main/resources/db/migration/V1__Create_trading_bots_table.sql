-- Создание таблицы торговых ботов
CREATE TABLE IF NOT EXISTS trading_bots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    figi VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    strategy VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    max_position_size NUMERIC(19, 2),
    min_price NUMERIC(19, 2),
    max_price NUMERIC(19, 2),
    sma_period INTEGER,
    ema_period INTEGER,
    created_at TIMESTAMP NOT NULL,
    last_execution TIMESTAMP,
    total_trades INTEGER DEFAULT 0,
    total_profit NUMERIC(19, 2) DEFAULT 0
);

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_trading_bots_user_id ON trading_bots(user_id);
CREATE INDEX IF NOT EXISTS idx_trading_bots_status ON trading_bots(status);
CREATE INDEX IF NOT EXISTS idx_trading_bots_figi ON trading_bots(figi);
