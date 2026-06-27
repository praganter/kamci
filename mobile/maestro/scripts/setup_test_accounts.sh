#!/bin/bash
# Gerçek backend'de 2 test hesabı oluşturur ve maestro/.env'e yazar
set -e
export PATH="$HOME/.maestro/bin:$PATH"

BASE_URL="http://198.51.100.10:30080"
TIMESTAMP=$(date +%s)
PASSWORD="TestPass123!"

# jq kontrolü
if ! command -v jq &>/dev/null; then
  echo "jq gerekli: brew install jq"
  exit 1
fi

echo "Test hesapları oluşturuluyor..."

# User A — Premium hesap
USER_A_EMAIL="kamci_a_${TIMESTAMP}@test.com"
RESPONSE_A=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_A_EMAIL\",\"password\":\"$PASSWORD\",\"premium\":true}")
TOKEN_A=$(echo "$RESPONSE_A" | jq -r '.accessToken // empty')
if [ -z "$TOKEN_A" ]; then
  echo "User A oluşturulamadı. Response: $RESPONSE_A"
  exit 1
fi
echo "$TOKEN_A" > /tmp/user_a_token.txt
echo "✓ User A: $USER_A_EMAIL"

# User B — Normal hesap
USER_B_EMAIL="kamci_b_${TIMESTAMP}@test.com"
RESPONSE_B=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_B_EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN_B=$(echo "$RESPONSE_B" | jq -r '.accessToken // empty')
if [ -z "$TOKEN_B" ]; then
  echo "User B oluşturulamadı. Response: $RESPONSE_B"
  exit 1
fi
echo "$TOKEN_B" > /tmp/user_b_token.txt
echo "✓ User B: $USER_B_EMAIL"

# maestro/.env yaz
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"
cat > "$ENV_FILE" <<EOF
USER_A_EMAIL=$USER_A_EMAIL
USER_A_PASSWORD=$PASSWORD
USER_B_EMAIL=$USER_B_EMAIL
USER_B_PASSWORD=$PASSWORD
EOF

echo ""
echo "✓ .env oluşturuldu: $ENV_FILE"
echo "  USER_A: $USER_A_EMAIL"
echo "  USER_B: $USER_B_EMAIL"
