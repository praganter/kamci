#!/bin/bash
# Creates test accounts A (premium), B and C on the local backend and writes maestro/.env
# Usage: API_BASE_URL=http://localhost:8080 ./maestro/scripts/setup_test_accounts.sh
set -e
export PATH="$HOME/.maestro/bin:$PATH"

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
ADMIN_KEY="${ADMIN_SECRET_KEY:-local-admin-key}"   # local compose default (application-local.yml)
TIMESTAMP=$(date +%s)
PASSWORD="${TEST_PASSWORD:-TestPass123!}"

if ! command -v jq &>/dev/null; then
  echo "jq is required (brew install jq / choco install jq)"
  exit 1
fi

# JWT 'sub' claim = user id
user_id_of() {
  python3 -c "import sys,json,base64;p=sys.argv[1].split('.')[1];p+='='*(-len(p)%4);print(json.loads(base64.urlsafe_b64decode(p))['sub'])" "$1"
}

register() { # $1 = email → prints access token
  curl -s -X POST "$BASE_URL/auth/register" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASSWORD\"}" | jq -r '.token // empty'
}

echo "Creating test accounts on $BASE_URL ..."

# User A — premium. Locally premium is granted through the admin endpoint
# (RevenueCat is not involved).
USER_A_EMAIL="kamci_a_${TIMESTAMP}@example.com"
TOKEN_A=$(register "$USER_A_EMAIL")
[ -z "$TOKEN_A" ] && { echo "User A could not be created"; exit 1; }
curl -s -X POST "$BASE_URL/subscription/premium/$(user_id_of "$TOKEN_A")" \
  -H "Authorization: Bearer $TOKEN_A" -H "X-Admin-Key: $ADMIN_KEY" >/dev/null
echo "✓ User A (premium): $USER_A_EMAIL"

USER_B_EMAIL="kamci_b_${TIMESTAMP}@example.com"
[ -z "$(register "$USER_B_EMAIL")" ] && { echo "User B could not be created"; exit 1; }
echo "✓ User B (free): $USER_B_EMAIL"

USER_C_EMAIL="kamci_c_${TIMESTAMP}@example.com"
[ -z "$(register "$USER_C_EMAIL")" ] && { echo "User C could not be created"; exit 1; }
echo "✓ User C (free): $USER_C_EMAIL"

# maestro/.env (git-ignored)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"
cat > "$ENV_FILE" <<ENV
API_BASE_URL=$BASE_URL
USER_A_EMAIL=$USER_A_EMAIL
USER_A_PASSWORD=$PASSWORD
USER_B_EMAIL=$USER_B_EMAIL
USER_B_PASSWORD=$PASSWORD
USER_C_EMAIL=$USER_C_EMAIL
USER_C_PASSWORD=$PASSWORD
ENV

echo ""
echo "✓ $ENV_FILE written"
