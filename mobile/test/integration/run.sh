#!/usr/bin/env bash
#
# Integration / E2E suite'ini login-only modda koşar.
#
# Kalıcı test hesapları maestro/.env'den okunur (USER_A = premium owner,
# USER_B = paylaşım üyesi) ve --dart-define olarak geçilir. Testler bu
# hesapları yalnızca LOGIN eder, register etmez.
#
# Kullanım:
#   test/integration/run.sh                 # tüm suite (register probe dahil)
#   test/integration/run.sh --exclude-tags register   # /auth/register'a hiç dokunma
#   test/integration/run.sh test/integration/debt      # tek dosya/dizin
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$REPO_ROOT/maestro/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "HATA: $ENV_FILE yok. maestro/.env.example'ı kopyalayın." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

# Argüman verilmediyse tüm integration dizini
TARGET_ARGS=("$@")
if [[ ${#TARGET_ARGS[@]} -eq 0 ]]; then
  TARGET_ARGS=(test/integration)
fi

cd "$REPO_ROOT"

exec flutter test "${TARGET_ARGS[@]}" -j1 \
  --dart-define=TEST_A_EMAIL="${USER_A_EMAIL:?}" \
  --dart-define=TEST_A_PASSWORD="${USER_A_PASSWORD:?}" \
  --dart-define=TEST_B_EMAIL="${USER_B_EMAIL:?}" \
  --dart-define=TEST_B_PASSWORD="${USER_B_PASSWORD:?}"
