#!/usr/bin/env bash
# E2E-02: Bot SMA round-trip (thin wrapper, ADR-001 / US-OSE-007)
# Запуск: docker compose -f docker-compose.test.yml --env-file .env.test.ose up -d && ./scripts/test-env/e2e-bot-roundtrip.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/e2e-trading-flow.sh" ose-02 "$@"
