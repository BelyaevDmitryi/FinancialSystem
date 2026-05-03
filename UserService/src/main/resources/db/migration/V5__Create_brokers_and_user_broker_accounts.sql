-- 1. Справочник брокеров (Tinkoff, VTB, Sber и т.д.)
CREATE TABLE IF NOT EXISTS brokers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Связующая таблица: пользователь ↔ брокеры (users 1 : N brokers)
-- Один пользователь может работать с несколькими брокерами
CREATE TABLE IF NOT EXISTS user_brokers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    broker_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_brokers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_brokers_broker FOREIGN KEY (broker_id) REFERENCES brokers(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_broker UNIQUE (user_id, broker_id)
);

-- 3. Счета у брокеров (broker 1 : N accounts — у брокера много счетов)
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    broker_id BIGINT NOT NULL,
    external_account_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_broker FOREIGN KEY (broker_id) REFERENCES brokers(id) ON DELETE CASCADE,
    CONSTRAINT uq_account_broker_external UNIQUE (broker_id, external_account_id)
);

-- 4. Связующая таблица: пользователь ↔ счета (users 1 : N accounts)
-- Один пользователь — много счетов; счёт привязан к пользователю через user_accounts
CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_accounts_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_account UNIQUE (user_id, account_id)
);

CREATE INDEX IF NOT EXISTS idx_user_brokers_user_id ON user_brokers(user_id);
CREATE INDEX IF NOT EXISTS idx_user_brokers_broker_id ON user_brokers(broker_id);
CREATE INDEX IF NOT EXISTS idx_accounts_broker_id ON accounts(broker_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_user_id ON user_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_account_id ON user_accounts(account_id);

-- Начальные данные: брокер Т-Инвестиции
INSERT INTO brokers (code, name) VALUES ('TINKOFF', 'Т-Инвестиции') ON CONFLICT (code) DO NOTHING;
