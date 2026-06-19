#!/usr/bin/env bash
# E2E-03: POST /api/backtest/run via ApiGateway.
# Requires: docker compose -f docker-compose.test.yml -f docker-compose.test-p1.yml --env-file .env.test-p1 up -d
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8090}"
E2E_USERNAME="${E2E_USERNAME:-e2e_test_user}"
E2E_PASSWORD="${E2E_PASSWORD:-E2eTestPass123!}"
FIGI="${FIGI:-BBG004730N88}"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

wait_gateway_ready() {
  local attempt=1
  while [ "$attempt" -le 30 ]; do
    if curl -sf "${GATEWAY_URL}/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  log_error "Gateway not ready at ${GATEWAY_URL}"
  exit 1
}

signin() {
  local out token
  out="$(curl -s -X POST "${GATEWAY_URL}/api/auth/signin" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${E2E_USERNAME}\",\"password\":\"${E2E_PASSWORD}\"}")"
  token="$(echo "$out" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)"
  if [ -z "${token}" ]; then
    log_error "signin failed: ${out}"
    exit 1
  fi
  echo "$token"
}

main() {
  log_info "E2E-03: backtest SMA via Gateway"
  wait_gateway_ready

  local token body code
  token="$(signin)"

  body="$(cat <<EOF
{
  "figi": "${FIGI}",
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-03-01T00:00:00Z",
  "interval": "DAY",
  "smaPeriod": 10,
  "initialCash": 100000,
  "slippageBps": 0
}
EOF
)"

  code="$(curl -s -o /tmp/e2e-backtest.json -w "%{http_code}" \
    -X POST "${GATEWAY_URL}/api/backtest/run" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${body}")"

  if [ "${code}" != "200" ]; then
    log_error "backtest failed HTTP ${code}: $(cat /tmp/e2e-backtest.json)"
    exit 1
  fi

  log_info "backtest OK: $(cat /tmp/e2e-backtest.json)"
}

main "$@"
