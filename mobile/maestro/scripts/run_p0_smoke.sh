#!/bin/bash
# P0 Smoke — Kritik akışlar (~10 dk, iOS+Android paralel)
# Kullanım:
#   ./run_p0_smoke.sh           # her iki platform (paralel)
#   ./run_p0_smoke.sh android   # sadece Android
#   ./run_p0_smoke.sh ios       # sadece iOS
set -e
export PATH="$HOME/.maestro/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAESTRO_DIR="$SCRIPT_DIR/.."
FLOWS="$MAESTRO_DIR/flows"
PLATFORM="${1:-both}"

if [ ! -f "$MAESTRO_DIR/.env" ]; then
  echo "Test hesapları oluşturuluyor..."
  "$SCRIPT_DIR/setup_test_accounts.sh"
fi

# .env yükle
set -a; source "$MAESTRO_DIR/.env"; set +a

ENV_FLAGS="--env USER_A_EMAIL=$USER_A_EMAIL --env USER_A_PASSWORD=$USER_A_PASSWORD --env USER_B_EMAIL=$USER_B_EMAIL --env USER_B_PASSWORD=$USER_B_PASSWORD"

# iOS: iPhone 16 (primary), iPhone 16 Plus (secondary/sharing)
IOS_DEVICE="15C5AAE6-5890-441F-9FA1-410AE0090B92"   # iPhone 16
ANDROID_DEVICE="emulator-5554"

P0_FLOWS=(
  "auth/login_valid.yaml"
  "auth/logout.yaml"
  "debt/create_one_time.yaml"
  "debt/pay_full.yaml"
  "debt/delete_debt.yaml"
  "category/create_category.yaml"
  "charts/charts_visible.yaml"
  "settings/settings_navigation.yaml"
  "premium/debt_limit_gate.yaml"
  "premium/premium_screen.yaml"
  # deeplink/* — real token gerektirir, manuel çalıştırılır
  "income/add_income.yaml"
)

run_flows() {
  local DEVICE=$1
  local PLATFORM_NAME=$2
  echo ""
  echo "========================================"
  echo "P0 Smoke: $PLATFORM_NAME ($DEVICE)"
  echo "========================================"
  for flow in "${P0_FLOWS[@]}"; do
    [[ "$flow" == \#* ]] && continue
    echo "→ $flow"
    maestro test --device "$DEVICE" $ENV_FLAGS "$FLOWS/$flow" || echo "❌ FAILED: $flow"
  done
  echo ""
  echo "✓ P0 Smoke tamamlandı: $PLATFORM_NAME"
}

case "$PLATFORM" in
  android)
    if adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
      run_flows "$ANDROID_DEVICE" "Android"
    else
      echo "❌ Android emülatör bulunamadı: $ANDROID_DEVICE"
      exit 1
    fi
    ;;
  ios)
    if xcrun simctl list devices booted | grep -q "$IOS_DEVICE"; then
      run_flows "$IOS_DEVICE" "iOS"
    else
      echo "❌ iPhone 16 simulatörü bulunamadı (UDID: $IOS_DEVICE). Boot edin."
      exit 1
    fi
    ;;
  both|*)
    PIDS=()
    if adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
      run_flows "$ANDROID_DEVICE" "Android" &
      PIDS+=($!)
    else
      echo "⚠️  Android emülatör bulunamadı — atlanıyor"
    fi
    if xcrun simctl list devices booted | grep -q "$IOS_DEVICE"; then
      run_flows "$IOS_DEVICE" "iOS" &
      PIDS+=($!)
    else
      echo "⚠️  iPhone 16 simulatörü bulunamadı — atlanıyor"
    fi
    for pid in "${PIDS[@]}"; do
      wait "$pid" || true
    done
    ;;
esac
