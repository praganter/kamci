#!/bin/bash
# P2 Edge Cases (~30 dk)
# Kullanım:
#   ./run_p2_edge_cases.sh           # her iki platform
#   ./run_p2_edge_cases.sh android   # sadece Android
#   ./run_p2_edge_cases.sh ios       # sadece iOS
set -e
export PATH="$HOME/.maestro/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAESTRO_DIR="$SCRIPT_DIR/.."
FLOWS="$MAESTRO_DIR/flows"
PLATFORM="${1:-both}"

if [ ! -f "$MAESTRO_DIR/.env" ]; then
  "$SCRIPT_DIR/setup_test_accounts.sh"
fi

set -a; source "$MAESTRO_DIR/.env"; set +a
ENV_FLAGS="--env USER_A_EMAIL=$USER_A_EMAIL --env USER_A_PASSWORD=$USER_A_PASSWORD --env USER_B_EMAIL=$USER_B_EMAIL --env USER_B_PASSWORD=$USER_B_PASSWORD"

IOS_DEVICE="15C5AAE6-5890-441F-9FA1-410AE0090B92"   # iPhone 16
ANDROID_DEVICE="emulator-5554"

P2_FLOWS=(
  "edge_cases/special_chars.yaml"
  "edge_cases/form_validation.yaml"
  "charts/charts_empty_state.yaml"
  "category/delete_with_debts.yaml"
  "deeplink/reset_password_complete.yaml"
  "notifications/fcm_register.yaml"
  "notifications/fcm_unregister_on_logout.yaml"
)

ANDROID_ONLY_FLOWS=(
  "edge_cases/offline_mode.yaml"
)

run_flows() {
  local DEVICE=$1
  local PLATFORM_NAME=$2
  local IS_ANDROID=$3
  echo ""
  echo "========================================"
  echo "P2 Edge Cases: $PLATFORM_NAME ($DEVICE)"
  echo "========================================"

  for flow in "${P2_FLOWS[@]}"; do
    echo "→ $flow"
    maestro test --device "$DEVICE" $ENV_FLAGS "$FLOWS/$flow" || echo "❌ FAILED: $flow"
  done

  if [ "$IS_ANDROID" = "true" ]; then
    echo ""
    echo "--- Android-only flows ---"
    for flow in "${ANDROID_ONLY_FLOWS[@]}"; do
      echo "→ $flow (ağ kapatılıyor...)"
      "$SCRIPT_DIR/toggle_network.sh" off "$DEVICE" android
      maestro test --device "$DEVICE" $ENV_FLAGS "$FLOWS/$flow" || echo "❌ FAILED: $flow"
      "$SCRIPT_DIR/toggle_network.sh" on "$DEVICE" android
    done
  else
    echo ""
    echo "⚠️  Offline testi (EDGE-01) iOS'ta atlandı — Manuel test: Simülatör > Features > Network Condition > 100% Loss"
  fi

  echo ""
  echo "✓ P2 Edge Cases tamamlandı: $PLATFORM_NAME"
}

case "$PLATFORM" in
  android)
    if adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
      run_flows "$ANDROID_DEVICE" "Android" "true"
    else
      echo "❌ Android emülatör bulunamadı: $ANDROID_DEVICE"
      exit 1
    fi
    ;;
  ios)
    if xcrun simctl list devices booted | grep -q "$IOS_DEVICE"; then
      run_flows "$IOS_DEVICE" "iOS" "false"
    else
      echo "❌ iPhone 16 simulatörü bulunamadı (UDID: $IOS_DEVICE)"
      exit 1
    fi
    ;;
  both|*)
    if adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
      run_flows "$ANDROID_DEVICE" "Android" "true"
    else
      echo "⚠️  Android emülatör bulunamadı — atlanıyor"
    fi
    if xcrun simctl list devices booted | grep -q "$IOS_DEVICE"; then
      run_flows "$IOS_DEVICE" "iOS" "false"
    else
      echo "⚠️  iPhone 16 simulatörü bulunamadı — atlanıyor"
    fi
    ;;
esac
