-- Historical OHLCV candles (OsEngine OsData analog); upsert by (figi, candle_interval, time)
CREATE TABLE IF NOT EXISTS candles (
    id BIGSERIAL PRIMARY KEY,
    figi VARCHAR(64) NOT NULL,
    candle_interval VARCHAR(32) NOT NULL,
    time TIMESTAMPTZ NOT NULL,
    open NUMERIC(19, 8) NOT NULL,
    high NUMERIC(19, 8) NOT NULL,
    low NUMERIC(19, 8) NOT NULL,
    close NUMERIC(19, 8) NOT NULL,
    volume BIGINT NOT NULL,
    CONSTRAINT uk_candles_figi_interval_time UNIQUE (figi, candle_interval, time)
);

CREATE INDEX IF NOT EXISTS idx_candles_figi_interval_time ON candles (figi, candle_interval, time);

-- Batch import job tracking (async ingestion in US-OSE-004 M2)
CREATE TABLE IF NOT EXISTS import_jobs (
    id BIGSERIAL PRIMARY KEY,
    figi VARCHAR(64) NOT NULL,
    candle_interval VARCHAR(32) NOT NULL,
    from_time TIMESTAMPTZ NOT NULL,
    to_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_status ON import_jobs (status);
CREATE INDEX IF NOT EXISTS idx_import_jobs_figi ON import_jobs (figi);
