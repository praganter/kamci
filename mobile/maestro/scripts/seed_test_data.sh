#!/bin/bash
# Test verisi oluştur: User B için 2 kategori + 5 borç (1 aylık taksitli)
# Çalıştır: ./seed_test_data.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env bulunamadı: $ENV_FILE"
  exit 1
fi
set -a; source "$ENV_FILE"; set +a

BASE_URL="http://198.51.100.10:30080"
TODAY=$(date +%Y-%m-%d)
# /debt/save firstPaymentDate LocalDateTime bekler; tarih-only → 400 body_not_readable
TODAY_DT="${TODAY}T00:00:00"

need_jq() {
  if ! command -v jq &>/dev/null; then
    echo "jq gerekli: brew install jq"
    exit 1
  fi
}
need_jq

auth() {
  local EMAIL=$1 PASS=$2
  curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" \
    | jq -r '.token // empty'
}

check_categories() {
  local TOKEN=$1
  curl -s "$BASE_URL/category/getAll" -H "Authorization: Bearer $TOKEN" | jq 'length'
}

check_debts() {
  local TOKEN=$1
  curl -s "$BASE_URL/debt/get?isOwner=true&month=$TODAY" \
    -H "Authorization: Bearer $TOKEN" | jq '.categories | map(.debts | length) | add // 0'
}

create_category() {
  local TOKEN=$1 NAME=$2
  curl -s -X POST "$BASE_URL/category/save" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$NAME\"}" | jq -r '.id // empty'
}

create_debt() {
  local TOKEN=$1 NAME=$2 CAT_ID=$3 PERIOD=$4 AMOUNT=$5
  curl -s -X POST "$BASE_URL/debt/save" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\":\"$NAME\",
      \"categoryId\":\"$CAT_ID\",
      \"amount\":$AMOUNT,
      \"currency\":\"TRY\",
      \"paymentPeriodType\":\"$PERIOD\",
      \"firstPaymentDate\":\"$TODAY_DT\",
      \"autoPayment\":false
    }" | jq -r '.debt.id // empty'
}

echo "=== Test Verisi Tohumu ==="
echo ""

# ── User B ────────────────────────────────────────────────────────────────────
echo "User B ($USER_B_EMAIL) için işlem yapılıyor..."
TOKEN_B=$(auth "$USER_B_EMAIL" "$USER_B_PASSWORD")
if [ -z "$TOKEN_B" ]; then echo "❌ User B giriş yapılamadı"; exit 1; fi

CAT_COUNT=$(check_categories "$TOKEN_B")
DEBT_COUNT=$(check_debts "$TOKEN_B")
echo "  Mevcut: $CAT_COUNT kategori, $DEBT_COUNT borç"

# Kategoriler: 2 adet gerekli (freemium limit = 2)
if [ "$CAT_COUNT" -lt 2 ]; then
  echo "  Kategoriler oluşturuluyor..."
  CAT1_ID=$(create_category "$TOKEN_B" "Ev Giderleri")
  CAT2_ID=$(create_category "$TOKEN_B" "Kişisel")
  if [ -n "$CAT1_ID" ]; then echo "  ✓ Kategori 1: $CAT1_ID"; fi
  if [ -n "$CAT2_ID" ]; then echo "  ✓ Kategori 2: $CAT2_ID"; fi
else
  echo "  ✓ Yeterli kategori mevcut ($CAT_COUNT)"
  # Mevcut kategori ID'lerini al
  CATS=$(curl -s "$BASE_URL/category/getAll" -H "Authorization: Bearer $TOKEN_B")
  CAT1_ID=$(echo "$CATS" | jq -r '.[0].id // empty')
  CAT2_ID=$(echo "$CATS" | jq -r '.[1].id // empty')
fi

# Borçlar: 5 adet gerekli (freemium limit = 5)
if [ "$DEBT_COUNT" -lt 5 ]; then
  NEEDED=$((5 - DEBT_COUNT))
  echo "  $NEEDED borç oluşturuluyor (5 limitine ulaşmak için)..."

  # 1 aylık taksitli borç (bulk_pay ve reminder gate testleri için zorunlu)
  if [ "$DEBT_COUNT" -lt 1 ]; then
    ID=$(create_debt "$TOKEN_B" "Kira" "$CAT1_ID" "MONTHLY" 5000)
    [ -n "$ID" ] && echo "  ✓ Aylık borç: $ID"
    DEBT_COUNT=$((DEBT_COUNT + 1))
  fi

  # Kalan tek seferlik borçlar
  NAMES=("Elektrik" "Su" "İnternet" "Telefon" "Sigorta")
  I=0
  while [ "$DEBT_COUNT" -lt 5 ] && [ "$I" -lt "${#NAMES[@]}" ]; do
    ID=$(create_debt "$TOKEN_B" "${NAMES[$I]}" "$CAT1_ID" "ONE_TIME" $((500 + I * 100)))
    [ -n "$ID" ] && echo "  ✓ Borç: ${NAMES[$I]} ($ID)"
    DEBT_COUNT=$((DEBT_COUNT + 1))
    I=$((I + 1))
  done
else
  echo "  ✓ Yeterli borç mevcut ($DEBT_COUNT)"
fi

FINAL_CATS=$(check_categories "$TOKEN_B")
FINAL_DEBTS=$(check_debts "$TOKEN_B")
echo ""
echo "✓ User B son durum: $FINAL_CATS kategori, $FINAL_DEBTS borç"

# ── User A ────────────────────────────────────────────────────────────────────
echo ""
echo "User A ($USER_A_EMAIL) kontrol ediliyor..."
TOKEN_A=$(auth "$USER_A_EMAIL" "$USER_A_PASSWORD")
if [ -z "$TOKEN_A" ]; then echo "❌ User A giriş yapılamadı"; exit 1; fi
PREMIUM_A=$(curl -s "$BASE_URL/auth/user-detail" -H "Authorization: Bearer $TOKEN_A" | jq -r '.premium')
echo "  Premium: $PREMIUM_A"
if [ "$PREMIUM_A" != "true" ]; then
  echo "  ⚠️  User A premium değil — PREM-07 testi atlanacak veya başarısız olacak"
  echo "  ℹ️  Premium aktivasyonu için gerçek cihazda App Store satın alımı gereklidir"
fi

echo ""
echo "✓ Seed tamamlandı."
