#!/usr/bin/env bash
# End-to-end smoke test through the gateway. Needs only bash + curl.
#   docker compose up -d --build --wait && scripts/smoke.sh
# Env: GATEWAY (default http://localhost:8080), MAILPIT (default http://localhost:8025)
set -euo pipefail

GATEWAY=${GATEWAY:-http://localhost:8080}
MAILPIT=${MAILPIT:-http://localhost:8025}
EMAIL="smoke-$(date +%s)-$$@example.com"
PASSWORD='Sm0ke-Passw0rd!'

step() { printf '\n== %s\n' "$*"; }
fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
# first value of a JSON string field: field id "$json"
field() { printf '%s' "$2" | grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
# req METHOD PATH [BODY] [TOKEN] -> sets $BODY, fails unless HTTP 2xx.
# 503 = the gateway has no instance for that route yet (Eureka cache lag, request never reached the service), so retry.
req() {
  local out i
  for i in $(seq 1 12); do
    out=$(curl -sS -m 20 -w '\n%{http_code}' -X "$1" "$GATEWAY$2" \
      -H 'Content-Type: application/json' ${4:+-H "Authorization: Bearer $4"} ${3:+-d "$3"}) || fail "$1 $2: curl error"
    [[ ${out##*$'\n'} == 503 ]] || break
    sleep 5
  done
  CODE=${out##*$'\n'}; BODY=${out%$'\n'*}
  [[ $CODE == 2* ]] || fail "$1 $2 -> HTTP $CODE: $BODY"
  echo "$1 $2 -> $CODE"
}

step "wait for gateway health"
for _ in $(seq 1 60); do curl -sf "$GATEWAY/actuator/health" >/dev/null && break; sleep 2; done
req GET /actuator/health; grep -q '"UP"' <<<"$BODY" || fail "gateway not UP: $BODY"

step "wait until the gateway can route to auth-service (Eureka cache lag: 503 = no instance yet)"
# Probe an unthrottled auth-service route; /auth/register is rate limited and 503s still burn its tokens.
for _ in $(seq 1 36); do
  [[ $(curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY/subscription/revenuecat/webhook" \
    -H 'Content-Type: application/json' -d '{}') != 503 ]] && break
  sleep 5
done

step "register"
req POST /auth/register "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"

step "login"
req POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
TOKEN=$(field token "$BODY"); [[ -n $TOKEN ]] || fail "no token in: $BODY"

step "unauthenticated call is rejected"
[[ $(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/category/getAll") == 401 ]] || fail "expected 401 without token"

step "create category"
req POST /category/save '{"name":"Smoke"}' "$TOKEN"
CAT=$(field id "$BODY"); [[ -n $CAT ]] || fail "no category id in: $BODY"

step "create debt"
FIRST=$(date -u -d 'yesterday' +%Y-%m-%dT%H:%M:%S 2>/dev/null || date -u -v-1d +%Y-%m-%dT%H:%M:%S)
req POST /debt/save "{\"name\":\"Smoke debt\",\"firstPaymentDate\":\"$FIRST\",\"installmentCount\":3,\"paymentPeriodType\":\"MONTHLY\",\"amount\":100,\"currency\":\"TRY\",\"categoryId\":\"$CAT\",\"autoPayment\":false}" "$TOKEN"
DEBT=$(field id "$BODY"); [[ -n $DEBT ]] || fail "no debt id in: $BODY"

step "list debts"
req GET "/debt/get?isOwner=true" "" "$TOKEN"
grep -q "$DEBT" <<<"$BODY" || fail "created debt $DEBT missing from list: $BODY"

step "pay"
req POST "/debt/pay/$DEBT" '{}' "$TOKEN"

step "payment history"
FROM=$(date -u -d '3 days ago' +%Y-%m-%dT%H:%M:%S 2>/dev/null || date -u -v-3d +%Y-%m-%dT%H:%M:%S)
TO=$(date -u -d '+2 days' +%Y-%m-%dT%H:%M:%S 2>/dev/null || date -u -v+2d +%Y-%m-%dT%H:%M:%S)
req GET "/debt/$DEBT/payments?from=$FROM&to=$TO" "" "$TOKEN"
[[ $BODY != "[]" ]] || fail "payment history is empty after paying"

step "mailpit: registration e-mail (soft check)"
for _ in $(seq 1 10); do
  curl -sf "$MAILPIT/api/v1/messages" 2>/dev/null | grep -q "$EMAIL" && { echo "mail to $EMAIL received"; break; } || sleep 2
done || true
curl -sf "$MAILPIT/api/v1/messages" 2>/dev/null | grep -q "$EMAIL" || echo "NOTE: no mail for $EMAIL in Mailpit (not a failure)"

printf '\nSMOKE OK\n'
