#!/usr/bin/env bash
# E2E тест торговых операций через ApiGateway
# Требует запущенного тестового стека: docker compose -f docker-compose.test.yml up -d
#
# Использование:
#   ./scripts/test-env/e2e-trading-flow.sh              # te-smoke (default)
#   ./scripts/test-env/e2e-trading-flow.sh ose-01     # RM1: MARKET → EXECUTED
#   ./scripts/test-env/e2e-trading-flow.sh ose-02-paper # paper bot → EXECUTED без broker
#   ./scripts/test-env/e2e-trading-flow.sh ose-06     # E2E-06: STOP create → cancel CANCELLED
#   E2E_SCENARIO=ose-01 ./scripts/test-env/e2e-trading-flow.sh
#
# Env-файлы: .env.test (minimal/PENDING) | .env.test.ose (RM1/EXECUTED, mock broker)
#
# Verify steps (US-OSE-001 M3):
#   te-smoke — шаги 0–5: health → signin → bot ACTIVE → order LIMIT PENDING (broker off)
#   ose-01   — шаги 0–6: MARKET EXECUTED + brokerOrderId; шаг 6: journal position (если US-OSE-003)
#   ose-02   — шаги 0–4: health → signin → bot ACTIVE; шаг 5: poll scheduler → EXECUTED; шаг 6: journal (опц.)
#   ose-02-paper — как ose-02, но paper=true: EXECUTED без brokerOrderId
#   ose-05   — шаги 0–5: MARKET EXECUTED; шаги 6–8: LIMIT PENDING → PATCH amend → POST cancel → CANCELLED
#   ose-06   — шаги 0–5: LIMIT PENDING; шаги 6–7: STOP PENDING → POST cancel → CANCELLED
#
# Переменные окружения:
#   GATEWAY_URL      — базовый URL ApiGateway (по умолчанию http://localhost:8090)
#   E2E_SCENARIO     — te-smoke | ose-01 | ose-02 | ose-02-paper | ose-05 | ose-06
#   E2E_USERNAME     — имя тестового пользователя
#   E2E_PASSWORD     — пароль тестового пользователя
#   E2E_POLL_INTERVAL — интервал poll для ose-02 (сек, по умолчанию 10)
#   E2E_POLL_MAX     — макс. попыток poll для ose-02 (по умолчанию 24, ~4 мин)
#   E2E_AUTH_WAIT_MAX — макс. ожидание готовности auth после health (сек, по умолчанию 90)
#   E2E_AUTH_WAIT_INTERVAL — интервал poll auth readiness (сек, по умолчанию 3)
#   SKIP_CLEANUP     — если "true", не удалять созданные данные

set -euo pipefail

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  sed -n '2,29p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
fi

if [ $# -gt 0 ] && [[ "$1" != -* ]]; then
  E2E_SCENARIO="$1"
  shift
fi

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8090}"
E2E_SCENARIO="${E2E_SCENARIO:-te-smoke}"
E2E_USERNAME="${E2E_USERNAME:-e2e_test_user}"
E2E_PASSWORD="${E2E_PASSWORD:-E2eTestPass123!}"
SKIP_CLEANUP="${SKIP_CLEANUP:-false}"
E2E_POLL_INTERVAL="${E2E_POLL_INTERVAL:-10}"
E2E_POLL_MAX="${E2E_POLL_MAX:-24}"
E2E_AUTH_WAIT_MAX="${E2E_AUTH_WAIT_MAX:-90}"
E2E_AUTH_WAIT_INTERVAL="${E2E_AUTH_WAIT_INTERVAL:-3}"
RUN_ID="${RUN_ID:-$(date +%s)-$$}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

CREATED_BOT_ID=""
CREATED_ORDER_ID=""
TOKEN=""

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

case "$E2E_SCENARIO" in
  te-smoke)
    EXPECTED_ORDER_STATUS="PENDING"
    ;;
  ose-01|ose-02|ose-02-paper)
    EXPECTED_ORDER_STATUS="EXECUTED"
    ;;
  ose-05)
    EXPECTED_ORDER_STATUS="EXECUTED"
    ;;
  ose-06)
    EXPECTED_ORDER_STATUS="PENDING"
    ;;
  *)
    log_error "Неизвестный E2E_SCENARIO: ${E2E_SCENARIO} (допустимо: te-smoke, ose-01, ose-02, ose-02-paper, ose-05, ose-06)"
    exit 1
    ;;
esac

if [ "$E2E_SCENARIO" = "ose-02" ]; then
  log_info "ose-02 (E2E-02): ACTIVE bot → scheduler → EXECUTED (poll GET /api/orders, .env.test.ose)"
fi

if [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
  log_info "ose-02-paper: paper bot → scheduler → EXECUTED без brokerOrderId (.env.test)"
fi

if [ "$E2E_SCENARIO" = "ose-05" ]; then
  log_info "ose-05 (E2E-05): MARKET EXECUTED → LIMIT amend → cancel CANCELLED"
fi

if [ "$E2E_SCENARIO" = "ose-06" ]; then
  log_info "ose-06 (E2E-06): LIMIT PENDING → STOP create → cancel CANCELLED"
fi

# curl с retry на 5xx и сетевые сбои (отказоустойчивость E2E)
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

# Ожидание маршрута auth через Gateway (UserService после compose up)
wait_for_auth_ready() {
  local max_wait="$E2E_AUTH_WAIT_MAX"
  local interval="$E2E_AUTH_WAIT_INTERVAL"
  local elapsed=0
  local out code

  log_info "Ожидание готовности auth через Gateway (до ${max_wait}s, интервал ${interval}s)..."
  while [ "$elapsed" -lt "$max_wait" ]; do
    out=$(curl -s -w "\n%{http_code}" -X POST \
      "${GATEWAY_URL}/api/auth/signin" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${E2E_USERNAME}\",\"password\":\"${E2E_PASSWORD}\"}" \
      2>/dev/null) || out=$'\n000'
    code=$(echo "$out" | tail -1)
    if [ "$code" = "200" ] || [ "$code" = "401" ]; then
      log_info "Auth path ready (HTTP ${code}) ✓"
      return 0
    fi
    if [ "$code" != "000" ] && [ "${code:0:1}" != "5" ]; then
      log_info "Auth path responded (HTTP ${code}) — считаем готовым ✓"
      return 0
    fi
    log_warn "Auth not ready (HTTP ${code}), retry in ${interval}s (${elapsed}/${max_wait}s elapsed)..."
    sleep "$interval"
    elapsed=$((elapsed + interval))
  done
  log_error "Auth path не готов за ${max_wait}s (последний HTTP ${code})"
  return 1
}

# totalTrades бота из JSON-массива GET /api/bots
bot_total_trades() {
  local bots_body="$1"
  local bot_id="$2"
  echo "$bots_body" | tr '{' '\n' | grep "\"id\":\"${bot_id}\"" | head -1 \
    | grep -o '"totalTrades":[0-9]*' | head -1 | cut -d':' -f2
}

# Ордер бота по уникальному имени E2E Bot ${RUN_ID} в comment
find_bot_order_in_list() {
  local orders_body="$1"
  local chunk
  chunk=$(echo "$orders_body" | tr '{' '\n' | grep "E2E Bot ${RUN_ID}" | head -1)
  if [ -z "$chunk" ]; then
    return 1
  fi
  ORDER_ID=$(echo "$chunk" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
  ORDER_STATUS=$(echo "$chunk" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
  [ -n "$ORDER_ID" ]
}

cleanup() {
  if [ "$SKIP_CLEANUP" = "true" ]; then
    log_warn "SKIP_CLEANUP=true — пропускаем очистку тестовых данных"
    return
  fi
  log_info "Очистка тестовых данных..."
  if [ -n "$CREATED_BOT_ID" ] && [ -n "$TOKEN" ]; then
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
      "${GATEWAY_URL}/api/bots/${CREATED_BOT_ID}" \
      -H "Authorization: Bearer ${TOKEN}" || echo "000")
    if [ "$HTTP_CODE" = "204" ]; then
      log_info "Бот ${CREATED_BOT_ID} удалён (cleanup)"
    else
      log_warn "Не удалось удалить бот ${CREATED_BOT_ID}: HTTP ${HTTP_CODE}"
    fi
  fi
}

trap cleanup EXIT

log_info "Сценарий: ${E2E_SCENARIO}, ожидаемый статус ордера: ${EXPECTED_ORDER_STATUS}, RUN_ID=${RUN_ID}"

# ─────────────────────────────────────────────
# Шаг 0: Health Gateway (retry)
# ─────────────────────────────────────────────
log_info "Шаг 0: Проверка health ApiGateway..."
HEALTH_OUT=$(curl_retry -f "${GATEWAY_URL}/actuator/health" || true)
HEALTH_CODE=$(echo "${HEALTH_OUT:-}" | tail -1)
if [ "$HEALTH_CODE" != "200" ]; then
  ENV_HINT=".env.test"
  if [ "$E2E_SCENARIO" = "ose-02" ] || [ "$E2E_SCENARIO" = "ose-02-paper" ] || [ "$E2E_SCENARIO" = "ose-01" ] || [ "$E2E_SCENARIO" = "ose-05" ] || [ "$E2E_SCENARIO" = "ose-06" ]; then
    ENV_HINT=".env.test.ose"
  fi
  if [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
    ENV_HINT=".env.test"
  fi
  log_error "Gateway недоступен (HTTP ${HEALTH_CODE}). Запустите: docker compose -f docker-compose.test.yml --env-file ${ENV_HINT} up -d"
  exit 1
fi
log_info "Gateway health OK ✓"

if ! wait_for_auth_ready; then
  log_error "Auth через Gateway недоступен. Дождитесь UserService или увеличьте E2E_AUTH_WAIT_MAX."
  exit 1
fi

# ─────────────────────────────────────────────
# Шаг 1: Регистрация тестового пользователя (idempotent)
# ─────────────────────────────────────────────
log_info "Шаг 1: Регистрация тестового пользователя (если не существует)..."
REGISTER_RESPONSE=$(curl_retry -X POST \
  "${GATEWAY_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${E2E_USERNAME}\",\"password\":\"${E2E_PASSWORD}\",\"email\":\"${E2E_USERNAME}@e2e.test\"}" || true)
REGISTER_CODE=$(echo "$REGISTER_RESPONSE" | tail -1)
if [ "$REGISTER_CODE" = "200" ] || [ "$REGISTER_CODE" = "201" ]; then
  log_info "Пользователь зарегистрирован (HTTP ${REGISTER_CODE})"
elif [ "$REGISTER_CODE" = "400" ] || [ "$REGISTER_CODE" = "409" ]; then
  log_info "Пользователь уже существует (HTTP ${REGISTER_CODE}) — продолжаем"
else
  log_warn "Регистрация вернула HTTP ${REGISTER_CODE} — пробуем войти"
fi

# ─────────────────────────────────────────────
# Шаг 2: Аутентификация
# ─────────────────────────────────────────────
log_info "Шаг 2: Аутентификация пользователя ${E2E_USERNAME}..."
SIGNIN_RESPONSE=$(curl_retry -X POST \
  "${GATEWAY_URL}/api/auth/signin" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${E2E_USERNAME}\",\"password\":\"${E2E_PASSWORD}\"}")
SIGNIN_BODY=$(echo "$SIGNIN_RESPONSE" | head -1)
SIGNIN_CODE=$(echo "$SIGNIN_RESPONSE" | tail -1)

if [ "$SIGNIN_CODE" != "200" ]; then
  log_error "Ошибка аутентификации: HTTP ${SIGNIN_CODE}"
  log_error "Ответ: ${SIGNIN_BODY}"
  exit 1
fi

TOKEN=$(echo "$SIGNIN_BODY" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 || \
        echo "$SIGNIN_BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  log_error "Не удалось извлечь токен из ответа: ${SIGNIN_BODY}"
  exit 1
fi
log_info "Токен получен (${#TOKEN} символов)"

# ─────────────────────────────────────────────
# Шаг 3: Создание торгового бота
# ─────────────────────────────────────────────
log_info "Шаг 3: Создание торгового бота (SMA_CROSSOVER)..."
if [ "$E2E_SCENARIO" = "ose-02" ]; then
  # US-OSE-009: fs.bot.default-paper=true — live E2E требует явного paper:false
  BOT_PAYLOAD="{\"figi\":\"BBG004730N88\",\"name\":\"E2E Bot ${RUN_ID}\",\"strategy\":\"SMA_CROSSOVER\",\"maxPositionSize\":1000,\"smaPeriod\":20,\"paper\":false}"
elif [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
  BOT_PAYLOAD="{\"figi\":\"BBG004730N88\",\"name\":\"E2E Bot ${RUN_ID}\",\"strategy\":\"SMA_CROSSOVER\",\"maxPositionSize\":1000,\"smaPeriod\":20,\"paper\":true}"
else
  BOT_PAYLOAD="{\"figi\":\"BBG004730N88\",\"name\":\"E2E Bot ${RUN_ID}\",\"strategy\":\"SMA_CROSSOVER\",\"maxPositionSize\":1000,\"smaPeriod\":20}"
fi

if [ "$E2E_SCENARIO" = "ose-02" ] || [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
  # P1 MarketHistory может кэшировать монотонные свечи mock-брокера — сброс для SMA SELL
  if command -v docker >/dev/null 2>&1; then
    docker exec financial-system-postgres-test psql -U postgres -d markethistorydb \
      -c "DELETE FROM candles WHERE figi='BBG004730N88';" >/dev/null 2>&1 || true
  fi
fi
BOT_RESPONSE=$(curl_retry -X POST \
  "${GATEWAY_URL}/api/bots" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "${BOT_PAYLOAD}")
BOT_BODY=$(echo "$BOT_RESPONSE" | head -1)
BOT_CODE=$(echo "$BOT_RESPONSE" | tail -1)

if [ "$BOT_CODE" != "201" ]; then
  log_error "Ошибка создания бота: HTTP ${BOT_CODE}"
  log_error "Ответ: ${BOT_BODY}"
  exit 1
fi

BOT_ID=$(echo "$BOT_BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
BOT_STATUS=$(echo "$BOT_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$BOT_ID" ]; then
  log_error "Не удалось извлечь ID бота из ответа: ${BOT_BODY}"
  exit 1
fi

CREATED_BOT_ID="$BOT_ID"
log_info "Бот создан: id=${BOT_ID}, status=${BOT_STATUS}"

if [ "$BOT_STATUS" != "ACTIVE" ]; then
  log_error "Ожидался статус ACTIVE, получено: ${BOT_STATUS}"
  exit 1
fi

# ─────────────────────────────────────────────
# Шаг 4: Проверка бота через GET /api/bots
# ─────────────────────────────────────────────
log_info "Шаг 4: Проверка созданного бота..."
BOTS_RESPONSE=$(curl_retry \
  "${GATEWAY_URL}/api/bots" \
  -H "Authorization: Bearer ${TOKEN}")
BOTS_BODY=$(echo "$BOTS_RESPONSE" | head -1)
BOTS_CODE=$(echo "$BOTS_RESPONSE" | tail -1)

if [ "$BOTS_CODE" != "200" ]; then
  log_error "Ошибка получения ботов: HTTP ${BOTS_CODE}"
  exit 1
fi

if ! echo "$BOTS_BODY" | grep -q "\"$BOT_ID\""; then
  log_error "Созданный бот ${BOT_ID} не найден в списке ботов"
  exit 1
fi
log_info "Бот ${BOT_ID} найден в списке ботов ✓"

# ─────────────────────────────────────────────
# Шаг 5: Ордер — ручной (te-smoke/ose-*) или poll scheduler (ose-02)
# ─────────────────────────────────────────────
ORDER_ID=""
ORDER_STATUS=""

if [ "$E2E_SCENARIO" = "ose-02" ] || [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
  if [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
    log_info "Шаг 5a: Seed paper MARKET BUY для открытия позиции..."
    SEED_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"BUY\",\"quantity\":1,\"price\":100.00,\"orderType\":\"MARKET\",\"paper\":true,\"comment\":\"E2E-seed-paper-${RUN_ID}\"}"
  else
    log_info "Шаг 5a: Seed MARKET BUY для открытия позиции (бот → SELL)..."
    SEED_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"BUY\",\"quantity\":1,\"price\":100.00,\"orderType\":\"MARKET\",\"comment\":\"E2E-seed-${RUN_ID}\"}"
  fi
  SEED_RESPONSE=$(curl_retry -X POST \
    "${GATEWAY_URL}/api/orders" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${SEED_PAYLOAD}")
  SEED_BODY=$(echo "$SEED_RESPONSE" | head -1)
  SEED_CODE=$(echo "$SEED_RESPONSE" | tail -1)
  if [ "$SEED_CODE" != "201" ]; then
    log_error "ose-02 seed BUY: HTTP ${SEED_CODE} — ${SEED_BODY}"
    exit 1
  fi
  SEED_STATUS=$(echo "$SEED_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [ "$SEED_STATUS" != "EXECUTED" ]; then
    log_error "ose-02 seed BUY: ожидался EXECUTED, получено ${SEED_STATUS}"
    exit 1
  fi
  log_info "Seed BUY исполнен ✓"

  log_info "Шаг 5: Ожидание ордера от scheduler бота (poll GET /api/orders, interval=${E2E_POLL_INTERVAL}s, max=${E2E_POLL_MAX})..."
  BOT_ORDER_FOUND=false
  poll_attempt=1

  while [ "$poll_attempt" -le "$E2E_POLL_MAX" ]; do
    ORDERS_RESPONSE=$(curl_retry \
      "${GATEWAY_URL}/api/orders" \
      -H "Authorization: Bearer ${TOKEN}")
    ORDERS_BODY=$(echo "$ORDERS_RESPONSE" | head -1)
    ORDERS_CODE=$(echo "$ORDERS_RESPONSE" | tail -1)

    if [ "$ORDERS_CODE" = "200" ] && find_bot_order_in_list "$ORDERS_BODY"; then
      CREATED_ORDER_ID="$ORDER_ID"
      if [ "$ORDER_STATUS" = "$EXPECTED_ORDER_STATUS" ]; then
        BOT_ORDER_FOUND=true
        log_info "Ордер от бота найден: id=${ORDER_ID}, status=${ORDER_STATUS} ✓"
        break
      fi
      log_info "Ордер от бота id=${ORDER_ID}, status=${ORDER_STATUS} — ждём ${EXPECTED_ORDER_STATUS}..."
    else
      BOTS_POLL=$(curl_retry \
        "${GATEWAY_URL}/api/bots" \
        -H "Authorization: Bearer ${TOKEN}")
      BOTS_POLL_BODY=$(echo "$BOTS_POLL" | head -1)
      BOTS_POLL_CODE=$(echo "$BOTS_POLL" | tail -1)
      if [ "$BOTS_POLL_CODE" = "200" ]; then
        TRADES=$(bot_total_trades "$BOTS_POLL_BODY" "$BOT_ID")
        if [ -n "$TRADES" ] && [ "$TRADES" -gt 0 ] 2>/dev/null; then
          log_info "Бот ${BOT_ID}: totalTrades=${TRADES} — ищем ордер в списке..."
        fi
      fi
    fi

    log_info "Попытка ${poll_attempt}/${E2E_POLL_MAX}: ордер ещё не готов, ждём ${E2E_POLL_INTERVAL}s..."
    sleep "$E2E_POLL_INTERVAL"
    poll_attempt=$((poll_attempt + 1))
  done

  if [ "$BOT_ORDER_FOUND" != "true" ]; then
    log_error "ose-02: scheduler не создал ордер со статусом ${EXPECTED_ORDER_STATUS} за $((E2E_POLL_MAX * E2E_POLL_INTERVAL))s"
    log_warn "Проверьте: .env.test.ose (BROKER_INTEGRATION_ENABLED=true), trading-bot-service health, S2S JWT, PriceService lookback для SMA"
    exit 1
  fi

  ORDER_CHUNK=$(echo "$ORDERS_BODY" | tr '{' '\n' | grep "E2E Bot ${RUN_ID}" | head -1 || true)
  BROKER_ORDER_ID=""
  if [ -n "$ORDER_CHUNK" ]; then
    BROKER_ORDER_ID=$(echo "$ORDER_CHUNK" | grep -o '"brokerOrderId":"[^"]*"' | head -1 | cut -d'"' -f4 || true)
  fi
  if [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
    if echo "$ORDER_CHUNK" | grep -q '"paper":true'; then
      log_info "paper=true на ордере бота ✓"
    else
      log_error "ose-02-paper: ожидался paper=true на ордере бота"
      exit 1
    fi
    if [ -n "$BROKER_ORDER_ID" ]; then
      log_error "ose-02-paper: brokerOrderId не должен быть задан для paper-ордера (${BROKER_ORDER_ID})"
      exit 1
    fi
    log_info "brokerOrderId отсутствует (paper) ✓"
  elif [ -z "$BROKER_ORDER_ID" ]; then
    log_error "ose-02: brokerOrderId отсутствует в ордере бота"
    exit 1
  else
    log_info "brokerOrderId=${BROKER_ORDER_ID} ✓"
  fi
else
  log_info "Шаг 5: Создание торгового ордера..."
  ORDER_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"BUY\",\"quantity\":1,\"price\":100.00,\"comment\":\"E2E-${RUN_ID}\"}"
  if [ "$E2E_SCENARIO" != "te-smoke" ]; then
    if [ "$E2E_SCENARIO" = "ose-06" ]; then
      ORDER_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"BUY\",\"quantity\":1,\"price\":90.00,\"orderType\":\"LIMIT\",\"comment\":\"E2E-LIMIT-${RUN_ID}\"}"
    else
      ORDER_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"BUY\",\"quantity\":1,\"price\":100.00,\"orderType\":\"MARKET\",\"comment\":\"E2E-${RUN_ID}\"}"
    fi
  fi

  ORDER_RESPONSE=$(curl_retry -X POST \
    "${GATEWAY_URL}/api/orders" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${ORDER_PAYLOAD}")
  ORDER_BODY=$(echo "$ORDER_RESPONSE" | head -1)
  ORDER_CODE=$(echo "$ORDER_RESPONSE" | tail -1)

  if [ "$ORDER_CODE" != "201" ]; then
    log_error "Ошибка создания ордера: HTTP ${ORDER_CODE}"
    log_error "Ответ: ${ORDER_BODY}"
    if [ "$E2E_SCENARIO" != "te-smoke" ] && [ "$ORDER_CODE" = "400" ]; then
      log_warn "Для ose-* нужен .env.test.ose (BROKER_INTEGRATION_ENABLED=true) и US-OSE-001 M2"
    fi
    exit 1
  fi

  ORDER_ID=$(echo "$ORDER_BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
  ORDER_STATUS=$(echo "$ORDER_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
  CREATED_ORDER_ID="$ORDER_ID"

  if [ -z "$ORDER_ID" ]; then
    log_error "Не удалось извлечь ID ордера из ответа: ${ORDER_BODY}"
    exit 1
  fi

  log_info "Ордер создан: id=${ORDER_ID}, status=${ORDER_STATUS}"

  if [ "$E2E_SCENARIO" = "ose-05" ]; then
    if [ "$ORDER_STATUS" != "EXECUTED" ]; then
      log_error "ose-05 шаг 5: ожидался MARKET EXECUTED, получено: ${ORDER_STATUS}"
      exit 1
    fi
    BROKER_ORDER_ID=$(echo "$ORDER_BODY" | grep -o '"brokerOrderId":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ -z "$BROKER_ORDER_ID" ]; then
      log_error "ose-05 шаг 5: brokerOrderId отсутствует в ответе"
      exit 1
    fi
    log_info "MARKET ордер исполнен, brokerOrderId=${BROKER_ORDER_ID} ✓"
  elif [ "$E2E_SCENARIO" = "ose-06" ]; then
    if [ "$ORDER_STATUS" != "PENDING" ]; then
      log_error "ose-06 шаг 5: ожидался LIMIT PENDING, получено: ${ORDER_STATUS}"
      exit 1
    fi
    log_info "LIMIT ордер PENDING (setup) ✓"
  elif [ "$ORDER_STATUS" != "$EXPECTED_ORDER_STATUS" ]; then
    log_error "Ожидался статус ${EXPECTED_ORDER_STATUS}, получено: ${ORDER_STATUS}"
    if [ "$E2E_SCENARIO" = "te-smoke" ]; then
      log_warn "Проверьте BROKER_INTEGRATION_ENABLED=false в .env.test"
    else
      log_warn "Проверьте .env.test.ose и mock broker (US-OSE-001)"
    fi
    exit 1
  fi
fi

# ─────────────────────────────────────────────
# Шаг 6–8 (ose-05): LIMIT → amend → cancel
# ─────────────────────────────────────────────
LIMIT_ORDER_ID=""
LIMIT_ORDER_STATUS=""
if [ "$E2E_SCENARIO" = "ose-05" ]; then
  log_info "Шаг 6: Создание LIMIT-ордера (ожидается PENDING)..."
  LIMIT_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"BUY\",\"quantity\":1,\"price\":90.00,\"orderType\":\"LIMIT\",\"comment\":\"E2E-LIMIT-${RUN_ID}\"}"
  LIMIT_RESPONSE=$(curl_retry -X POST \
    "${GATEWAY_URL}/api/orders" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${LIMIT_PAYLOAD}")
  LIMIT_BODY=$(echo "$LIMIT_RESPONSE" | head -1)
  LIMIT_CODE=$(echo "$LIMIT_RESPONSE" | tail -1)

  if [ "$LIMIT_CODE" != "201" ]; then
    log_error "ose-05 шаг 6: ошибка создания LIMIT: HTTP ${LIMIT_CODE}"
    log_error "Ответ: ${LIMIT_BODY}"
    exit 1
  fi

  LIMIT_ORDER_ID=$(echo "$LIMIT_BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
  LIMIT_ORDER_STATUS=$(echo "$LIMIT_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [ "$LIMIT_ORDER_STATUS" != "PENDING" ]; then
    log_error "ose-05 шаг 6: ожидался PENDING, получено: ${LIMIT_ORDER_STATUS}"
    exit 1
  fi
  log_info "LIMIT ордер создан: id=${LIMIT_ORDER_ID}, status=${LIMIT_ORDER_STATUS} ✓"

  log_info "Шаг 7: Изменение цены LIMIT (PATCH /api/orders/${LIMIT_ORDER_ID})..."
  AMEND_RESPONSE=$(curl_retry -X PATCH \
    "${GATEWAY_URL}/api/orders/${LIMIT_ORDER_ID}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"price":95.00}')
  AMEND_BODY=$(echo "$AMEND_RESPONSE" | head -1)
  AMEND_CODE=$(echo "$AMEND_RESPONSE" | tail -1)

  if [ "$AMEND_CODE" != "200" ]; then
    log_error "ose-05 шаг 7: ошибка amend: HTTP ${AMEND_CODE}"
    log_error "Ответ: ${AMEND_BODY}"
    exit 1
  fi

  AMENDED_PRICE=$(echo "$AMEND_BODY" | grep -o '"price":[0-9.]*' | head -1 | cut -d':' -f2)
  if [ "$AMENDED_PRICE" != "95.00" ] && [ "$AMENDED_PRICE" != "95" ]; then
    log_error "ose-05 шаг 7: ожидалась цена 95.00, получено: ${AMENDED_PRICE}"
    exit 1
  fi
  log_info "LIMIT amend OK, price=${AMENDED_PRICE} ✓"

  log_info "Шаг 8: Отмена LIMIT (POST /api/orders/${LIMIT_ORDER_ID}/cancel)..."
  CANCEL_RESPONSE=$(curl_retry -X POST \
    "${GATEWAY_URL}/api/orders/${LIMIT_ORDER_ID}/cancel" \
    -H "Authorization: Bearer ${TOKEN}")
  CANCEL_BODY=$(echo "$CANCEL_RESPONSE" | head -1)
  CANCEL_CODE=$(echo "$CANCEL_RESPONSE" | tail -1)

  if [ "$CANCEL_CODE" != "200" ]; then
    log_error "ose-05 шаг 8: ошибка cancel: HTTP ${CANCEL_CODE}"
    log_error "Ответ: ${CANCEL_BODY}"
    exit 1
  fi

  CANCEL_STATUS=$(echo "$CANCEL_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [ "$CANCEL_STATUS" != "CANCELLED" ]; then
    log_error "ose-05 шаг 8: ожидался CANCELLED, получено: ${CANCEL_STATUS}"
    exit 1
  fi
  LIMIT_ORDER_STATUS="$CANCEL_STATUS"
  log_info "LIMIT ордер отменён: status=${CANCEL_STATUS} ✓"
fi

# ─────────────────────────────────────────────
# Шаг 6–7 (ose-06): STOP → cancel
# ─────────────────────────────────────────────
STOP_ORDER_ID=""
STOP_ORDER_STATUS=""
if [ "$E2E_SCENARIO" = "ose-06" ]; then
  log_info "Шаг 6: Создание STOP-ордера (ожидается PENDING)..."
  STOP_PAYLOAD="{\"figi\":\"BBG004730N88\",\"type\":\"SELL\",\"quantity\":1,\"price\":100.00,\"orderType\":\"STOP\",\"stopPrice\":95.00,\"comment\":\"E2E-STOP-${RUN_ID}\"}"
  STOP_RESPONSE=$(curl_retry -X POST \
    "${GATEWAY_URL}/api/orders" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${STOP_PAYLOAD}")
  STOP_BODY=$(echo "$STOP_RESPONSE" | head -1)
  STOP_CODE=$(echo "$STOP_RESPONSE" | tail -1)

  if [ "$STOP_CODE" != "201" ]; then
    log_error "ose-06 шаг 6: ошибка создания STOP: HTTP ${STOP_CODE}"
    log_error "Ответ: ${STOP_BODY}"
    exit 1
  fi

  STOP_ORDER_ID=$(echo "$STOP_BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
  STOP_ORDER_STATUS=$(echo "$STOP_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [ "$STOP_ORDER_STATUS" != "PENDING" ]; then
    log_error "ose-06 шаг 6: ожидался PENDING, получено: ${STOP_ORDER_STATUS}"
    exit 1
  fi
  log_info "STOP ордер создан: id=${STOP_ORDER_ID}, status=${STOP_ORDER_STATUS} ✓"

  log_info "Шаг 7: Отмена STOP (POST /api/orders/${STOP_ORDER_ID}/cancel)..."
  STOP_CANCEL_RESPONSE=$(curl_retry -X POST \
    "${GATEWAY_URL}/api/orders/${STOP_ORDER_ID}/cancel" \
    -H "Authorization: Bearer ${TOKEN}")
  STOP_CANCEL_BODY=$(echo "$STOP_CANCEL_RESPONSE" | head -1)
  STOP_CANCEL_CODE=$(echo "$STOP_CANCEL_RESPONSE" | tail -1)

  if [ "$STOP_CANCEL_CODE" != "200" ]; then
    log_error "ose-06 шаг 7: ошибка cancel: HTTP ${STOP_CANCEL_CODE}"
    log_error "Ответ: ${STOP_CANCEL_BODY}"
    exit 1
  fi

  STOP_CANCEL_STATUS=$(echo "$STOP_CANCEL_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [ "$STOP_CANCEL_STATUS" != "CANCELLED" ]; then
    log_error "ose-06 шаг 7: ожидался CANCELLED, получено: ${STOP_CANCEL_STATUS}"
    exit 1
  fi
  STOP_ORDER_STATUS="$STOP_CANCEL_STATUS"
  log_info "STOP ордер отменён: status=${STOP_CANCEL_STATUS} ✓"
fi

# ─────────────────────────────────────────────
# Шаг 6 (ose-01, ose-02): Journal position
# ─────────────────────────────────────────────
if [ "$E2E_SCENARIO" = "ose-01" ] || [ "$E2E_SCENARIO" = "ose-02" ] || [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
  log_info "Шаг 6: Проверка позиции в Journal (E2E-01/02)..."
  JOURNAL_RESPONSE=$(curl_retry \
    "${GATEWAY_URL}/api/journal/positions/BBG004730N88" \
    -H "Authorization: Bearer ${TOKEN}" 2>/dev/null || echo -e "\n000")
  JOURNAL_CODE=$(echo "$JOURNAL_RESPONSE" | tail -1)
  JOURNAL_BODY=$(echo "$JOURNAL_RESPONSE" | head -1)

  if [ "$JOURNAL_CODE" = "404" ] || [ "$JOURNAL_CODE" = "000" ] || [ "$JOURNAL_CODE" = "503" ]; then
    log_warn "Journal API недоступен (HTTP ${JOURNAL_CODE}) — ожидается после US-OSE-003"
  elif [ "$JOURNAL_CODE" = "200" ]; then
    if [ "$E2E_SCENARIO" = "ose-02" ] || [ "$E2E_SCENARIO" = "ose-02-paper" ]; then
      TRADES_RESPONSE=$(curl_retry \
        "${GATEWAY_URL}/api/journal/trades" \
        -H "Authorization: Bearer ${TOKEN}" 2>/dev/null || echo -e "\n000")
      TRADES_CODE=$(echo "$TRADES_RESPONSE" | tail -1)
      TRADES_BODY=$(echo "$TRADES_RESPONSE" | head -1)
      if [ "$TRADES_CODE" = "200" ] && echo "$TRADES_BODY" | grep -q "BBG004730N88"; then
        log_info "Сделки в Journal найдены ✓"
      else
        log_error "Journal trades недоступны или пусты (HTTP ${TRADES_CODE}): ${TRADES_BODY}"
        exit 1
      fi
    elif echo "$JOURNAL_BODY" | grep -qE '"quantity":[1-9]'; then
      log_info "Позиция в Journal найдена ✓"
    else
      log_error "Journal вернул 200, но позиция не найдена: ${JOURNAL_BODY}"
      exit 1
    fi
  else
    log_error "Неожиданный ответ Journal: HTTP ${JOURNAL_CODE} — ${JOURNAL_BODY}"
    exit 1
  fi
fi

# ─────────────────────────────────────────────
# Итог
# ─────────────────────────────────────────────
echo ""
log_info "══════════════════════════════════════════"
log_info "E2E (${E2E_SCENARIO}) завершён успешно!"
log_info "  Бот:   id=${BOT_ID}, status=ACTIVE"
log_info "  Ордер: id=${ORDER_ID}, status=${ORDER_STATUS}"
if [ "$E2E_SCENARIO" = "ose-05" ]; then
  log_info "  LIMIT: id=${LIMIT_ORDER_ID}, status=${LIMIT_ORDER_STATUS}"
fi
if [ "$E2E_SCENARIO" = "ose-06" ]; then
  log_info "  STOP:  id=${STOP_ORDER_ID}, status=${STOP_ORDER_STATUS}"
fi
log_info "══════════════════════════════════════════"
