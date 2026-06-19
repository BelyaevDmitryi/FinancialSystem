#!/usr/bin/env bash
# E2E-04: POST /api/bot-optimization/grid via ApiGateway (P1).
# Requires: docker compose -f docker-compose.test.yml -f docker-compose.test-p1.yml --env-file .env.test-p1 up -d
# Свечи: сначала ./scripts/test-env/seed-candles.sh (MarketHistory для figi/диапазона from..to).
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8090}"
E2E_USERNAME="${E2E_USERNAME:-e2e_test_user}"
E2E_PASSWORD="${E2E_PASSWORD:-E2eTestPass123!}"
FIGI="${FIGI:-BBG004730N88}"
FROM="${FROM:-2026-01-01T00:00:00Z}"
TO="${TO:-2026-03-01T00:00:00Z}"
RESPONSE_FILE="${RESPONSE_FILE:-/tmp/e2e-optimize.json}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

curl_retry() {
  local max_attempts=5
  local attempt=1
  local delay=2
  local out code

  while [ "$attempt" -le "$max_attempts" ]; do
    out=$(curl -s -w "\n%{http_code}" "$@" 2>/dev/null) || out=$'\n000'
    code=$(echo "$out" | tail -1)
    if [ "$code" != "000" ] && [ "${code:0:1}" != "5" ]; then
      echo "$out"
      return 0
    fi
    log_warn "curl retry ${attempt}/${max_attempts} (HTTP ${code})"
    sleep "$delay"
    attempt=$((attempt + 1))
    delay=$((delay * 2))
  done
  echo "$out"
  return 1
}

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
  out="$(curl_retry -X POST "${GATEWAY_URL}/api/auth/signin" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${E2E_USERNAME}\",\"password\":\"${E2E_PASSWORD}\"}")"
  token="$(echo "$out" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)"
  if [ -z "${token}" ]; then
    token="$(echo "$out" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)"
  fi
  if [ -z "${token}" ]; then
    log_error "signin failed: ${out}"
    exit 1
  fi
  echo "$token"
}

assert_grid_response() {
  local body="$1"
  local total_runs

  total_runs="$(echo "$body" | grep -o '"totalRuns":[0-9]*' | head -1 | cut -d':' -f2)"
  if [ -z "${total_runs}" ] || [ "${total_runs}" -lt 6 ]; then
    log_error "expected totalRuns >= 6, got: ${total_runs:-<missing>}"
    exit 1
  fi
  if ! echo "$body" | grep -q '"results":\['; then
    log_error "response missing results array"
    exit 1
  fi
  if ! echo "$body" | grep -q '"parameters":'; then
    log_error "results array is empty (no run entries)"
    exit 1
  fi
}

main() {
  log_info "E2E-04: multi-param grid optimization via Gateway"
  log_info "Ensure candles are seeded: ./scripts/test-env/seed-candles.sh"
  wait_gateway_ready

  local token body raw http_code response_body
  token="$(signin)"

  body="$(cat <<EOF
{
  "figi": "${FIGI}",
  "from": "${FROM}",
  "to": "${TO}",
  "interval": "DAY",
  "initialCash": 100000,
  "slippageBps": 0,
  "parameters": [
    {
      "name": "smaPeriod",
      "min": 10,
      "max": 20,
      "step": 2,
      "stepType": "ABSOLUTE"
    }
  ],
  "filters": {
    "minProfitFactor": 0,
    "maxDrawdown": 1,
    "minTrades": 0
  }
}
EOF
)"

  raw="$(curl_retry -X POST "${GATEWAY_URL}/api/bot-optimization/grid" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${body}")"
  http_code="$(echo "$raw" | tail -1)"
  response_body="$(echo "$raw" | sed '$d')"
  echo "${response_body}" > "${RESPONSE_FILE}"

  if [ "${http_code}" != "200" ]; then
    log_error "grid optimize failed HTTP ${http_code}: ${response_body}"
    exit 1
  fi

  assert_grid_response "${response_body}"
  log_info "grid optimize OK (totalRuns>=6, results non-empty): ${response_body}"
}

main "$@"
