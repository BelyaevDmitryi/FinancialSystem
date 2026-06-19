#!/usr/bin/env bash
# Загрузка исторических свечей в MarketHistoryService (P1 compose).
# Требует: docker compose -f docker-compose.test.yml -f docker-compose.test-p1.yml --env-file .env.test-p1 up -d
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8090}"
MARKET_HISTORY_URL="${MARKET_HISTORY_URL:-http://localhost:8010}"
USE_GATEWAY="${USE_GATEWAY:-true}"
E2E_USERNAME="${E2E_USERNAME:-e2e_test_user}"
E2E_PASSWORD="${E2E_PASSWORD:-E2eTestPass123!}"
FIGI="${FIGI:-BBG004730N88}"
FROM="${FROM:-2026-01-01T00:00:00Z}"
TO="${TO:-2026-03-01T00:00:00Z}"
INTERVAL="${INTERVAL:-DAY}"
POLL_SECONDS="${POLL_SECONDS:-2}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-60}"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

base_url() {
  if [ "${USE_GATEWAY}" = "true" ]; then
    echo "${GATEWAY_URL}/api/market-history"
  else
    echo "${MARKET_HISTORY_URL}/market-history"
  fi
}

wait_health() {
  local url attempt=1
  if [ "${USE_GATEWAY}" = "true" ]; then
    url="${GATEWAY_URL}/actuator/health"
  else
    url="${MARKET_HISTORY_URL}/actuator/health"
  fi
  while [ "$attempt" -le 30 ]; do
    if curl -sf "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  log_error "Market history stack not ready (${url})"
  exit 1
}

signup() {
  local code
  code="$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY_URL}/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${E2E_USERNAME}\",\"password\":\"${E2E_PASSWORD}\",\"email\":\"${E2E_USERNAME}@e2e.test\"}" \
    2>/dev/null || echo "000")"
  if [ "$code" = "200" ] || [ "$code" = "201" ]; then
    log_info "user registered (HTTP ${code})"
  elif [ "$code" = "400" ] || [ "$code" = "409" ]; then
    log_info "user already exists (HTTP ${code})"
  else
    log_info "signup returned HTTP ${code} — will try signin"
  fi
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

auth_header() {
  if [ "${USE_GATEWAY}" = "true" ]; then
    echo "Authorization: Bearer $(signin)"
  fi
}

main() {
  log_info "Seed candles: figi=${FIGI} ${FROM}..${TO} interval=${INTERVAL}"
  wait_health

  local api token auth_args=()
  api="$(base_url)"
  if [ "${USE_GATEWAY}" = "true" ]; then
    signup
    token="$(signin)"
    auth_args=(-H "Authorization: Bearer ${token}")
  fi

  local import_body job_id status attempt=1
  import_body="$(cat <<EOF
{"figi":"${FIGI}","from":"${FROM}","to":"${TO}","interval":"${INTERVAL}"}
EOF
)"

  job_id="$(curl -s "${auth_args[@]}" -X POST "${api}/import" \
    -H "Content-Type: application/json" \
    -d "${import_body}" | grep -o '"jobId":[0-9]*' | head -1 | cut -d':' -f2)"

  if [ -z "${job_id}" ]; then
    log_error "import start failed"
    exit 1
  fi
  log_info "import jobId=${job_id}"

  while [ "$attempt" -le "${MAX_ATTEMPTS}" ]; do
    status="$(curl -s "${auth_args[@]}" "${api}/import/${job_id}" \
      | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)"
    if [ "${status}" = "COMPLETED" ]; then
      log_info "import COMPLETED (jobId=${job_id})"
      exit 0
    fi
    if [ "${status}" = "FAILED" ]; then
      log_error "import FAILED (jobId=${job_id})"
      curl -s "${auth_args[@]}" "${api}/import/${job_id}" || true
      exit 1
    fi
    sleep "${POLL_SECONDS}"
    attempt=$((attempt + 1))
  done

  log_error "import timeout (jobId=${job_id}, last status=${status:-unknown})"
  exit 1
}

main "$@"
