-- Список счетов пользователя у брокера хранится в связке UserBroker (таблица user_broker_accounts).
-- Выбранный счёт — в user_brokers.default_account_id.

-- 1. Таблица счетов пользователя у брокера (список счетов в сущности "пользователь у брокера")
CREATE TABLE IF NOT EXISTS user_broker_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_broker_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_broker_accounts_user_broker FOREIGN KEY (user_broker_id) REFERENCES user_brokers(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_broker_accounts_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_broker_account UNIQUE (user_broker_id, account_id)
);

-- 2. Поле "выбранный счёт" в связке пользователь–брокер (быстрый доступ к счёту по умолчанию)
ALTER TABLE user_brokers
    ADD COLUMN IF NOT EXISTS default_account_id BIGINT NULL,
    ADD CONSTRAINT fk_user_brokers_default_account FOREIGN KEY (default_account_id) REFERENCES accounts(id) ON DELETE SET NULL;

-- 3. Перенос данных из user_accounts в user_broker_accounts
INSERT INTO user_broker_accounts (user_broker_id, account_id, is_default, created_at)
SELECT ub.id, ua.account_id, ua.is_default, ua.created_at
FROM user_accounts ua
JOIN accounts a ON a.id = ua.account_id
JOIN user_brokers ub ON ub.user_id = ua.user_id AND ub.broker_id = a.broker_id
ON CONFLICT (user_broker_id, account_id) DO NOTHING;

-- 4. Заполняем выбранный счёт в user_brokers (где is_default = true в user_broker_accounts)
UPDATE user_brokers ub
SET default_account_id = uba.account_id
FROM user_broker_accounts uba
WHERE uba.user_broker_id = ub.id AND uba.is_default = true;

-- 5. Удаляем старую связь пользователь–счёт (список теперь у UserBroker)
DROP TABLE IF EXISTS user_accounts;

CREATE INDEX IF NOT EXISTS idx_user_broker_accounts_user_broker_id ON user_broker_accounts(user_broker_id);
CREATE INDEX IF NOT EXISTS idx_user_broker_accounts_account_id ON user_broker_accounts(account_id);
CREATE INDEX IF NOT EXISTS idx_user_brokers_default_account_id ON user_brokers(default_account_id);
