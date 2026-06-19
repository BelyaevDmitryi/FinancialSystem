-- Поля lifecycle заявки у брокера (US-OSE-001 M1)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS broker_order_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS broker_code VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS stop_price NUMERIC(19, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS broker_status VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_orders_broker_order_id ON orders(broker_order_id);
