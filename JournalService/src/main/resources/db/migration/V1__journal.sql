-- Trade journal: fills from TradingTerminal (idempotent by order_id, ADR-002 §4)
CREATE TABLE IF NOT EXISTS trades (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    figi VARCHAR(255) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    price NUMERIC(19, 8) NOT NULL,
    realized_pnl NUMERIC(19, 8),
    order_id BIGINT NOT NULL,
    commission NUMERIC(19, 8),
    executed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_trades_order_id UNIQUE (order_id)
);

CREATE INDEX IF NOT EXISTS idx_trades_user_id ON trades(user_id);
CREATE INDEX IF NOT EXISTS idx_trades_figi ON trades(figi);
CREATE INDEX IF NOT EXISTS idx_trades_user_figi ON trades(user_id, figi);
CREATE INDEX IF NOT EXISTS idx_trades_executed_at ON trades(executed_at);

-- Open positions per user and instrument (P0: one position per user+figi)
CREATE TABLE IF NOT EXISTS positions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    figi VARCHAR(255) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL DEFAULT 0,
    avg_price NUMERIC(19, 8) NOT NULL DEFAULT 0,
    CONSTRAINT uk_positions_user_figi UNIQUE (user_id, figi)
);

CREATE INDEX IF NOT EXISTS idx_positions_user_id ON positions(user_id);
