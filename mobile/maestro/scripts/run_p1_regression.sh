#!/bin/bash
# P1 Regression — Tam suite (~45 dk, iOS+Android paralel)
# Kullanım:
#   ./run_p1_regression.sh           # her iki platform + sharing (paralel)
#   ./run_p1_regression.sh android   # sadece Android + sharing
#   ./run_p1_regression.sh ios       # sadece iOS + sharing
set -e
export PATH="$HOME/.maestro/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAESTRO_DIR="$SCRIPT_DIR/.."
FLOWS="$MAESTRO_DIR/flows"
PLATFORM="${1:-both}"

# iOS Simulator UIKit animasyonlarını 100x hızlandır (Flutter animasyonları için
# uygulamayı --dart-define=MAESTRO=true ile build et)
disable_ios_animations() {
  local udid=$1
  xcrun simctl spawn "$udid" launchctl setenv UIAnimationDragCoefficient 0.01 2>/dev/null || true
}

if [ ! -f "$MAESTRO_DIR/.env" ]; then
  "$SCRIPT_DIR/setup_test_accounts.sh"
fi

set -a; source "$MAESTRO_DIR/.env"; set +a
ENV_FLAGS="--env USER_A_EMAIL=$USER_A_EMAIL --env USER_A_PASSWORD=$USER_A_PASSWORD --env USER_B_EMAIL=$USER_B_EMAIL --env USER_B_PASSWORD=$USER_B_PASSWORD"
MAESTRO_FLAGS="--no-reinstall-driver"

IOS_DEVICE="15C5AAE6-5890-441F-9FA1-410AE0090B92"   # iPhone 16
ANDROID_DEVICE="emulator-5554"

SUITES=(
  "auth"
  "debt"
  "category"
  "income"
  "charts"
  "settings"
  "premium"
  # "deeplink"  # BLOCKED: Gerçek reset token gerektirir — manuel test (deeplink/*.yaml dosyalarındaki NOT bakınız)
  "edge_cases"
  "notifications"
  "household"
)

# Sonuçları geçici dosyaya yaz (paralel subshell'den FAILED array'e yazamayız)
FAIL_DIR=$(mktemp -d)

run_suite() {
  local DEVICE=$1
  local PLATFORM_NAME=$2
  local FAIL_FILE="$FAIL_DIR/$PLATFORM_NAME.txt"
  echo ""
  echo "========================================"
  echo "P1 Regression: $PLATFORM_NAME ($DEVICE)"
  echo "========================================"
  for suite in "${SUITES[@]}"; do
    [[ "$suite" == \#* ]] && continue
    echo ""
    echo "--- Suite: $suite ---"
    for flow_file in "$FLOWS/$suite"/*.yaml; do
      [ -f "$flow_file" ] || continue
      flow_name=$(basename "$flow_file")
      echo "→ [$PLATFORM_NAME] $suite/$flow_name"
      maestro test --device "$DEVICE" $ENV_FLAGS $MAESTRO_FLAGS "$flow_file" || {
        echo "❌ FAILED: $suite/$flow_name"
        echo "$PLATFORM_NAME: $suite/$flow_name" >> "$FAIL_FILE"
      }
    done
  done
}

case "$PLATFORM" in
  android)
    if adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
      run_suite "$ANDROID_DEVICE" "Android"
      "$SCRIPT_DIR/run_sharing_tests.sh" android
    else
      echo "❌ Android emülatör bulunamadı: $ANDROID_DEVICE"
      exit 1
    fi
    ;;
  ios)
    if xcrun simctl list devices booted | grep -q "$IOS_DEVICE"; then
      disable_ios_animations "$IOS_DEVICE"
      run_suite "$IOS_DEVICE" "iOS"
      "$SCRIPT_DIR/run_sharing_tests.sh" ios
    else
      echo "❌ iPhone 16 simulatörü bulunamadı (UDID: $IOS_DEVICE)"
      exit 1
    fi
    ;;
  both|*)
    ANDROID_AVAILABLE=false
    IOS_AVAILABLE=false
    PIDS=()

    if adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
      ANDROID_AVAILABLE=true
      run_suite "$ANDROID_DEVICE" "Android" &
      PIDS+=($!)
    else
      echo "⚠️  Android emülatör bulunamadı — atlanıyor"
    fi

    if xcrun simctl list devices booted | grep -q "$IOS_DEVICE"; then
      IOS_AVAILABLE=true
      disable_ios_animations "$IOS_DEVICE"
      run_suite "$IOS_DEVICE" "iOS" &
      PIDS+=($!)
    else
      echo "⚠️  iPhone 16 simulatörü bulunamadı — atlanıyor"
    fi

    # Her iki platform tamamlanana kadar bekle
    for pid in "${PIDS[@]}"; do
      wait "$pid" || true
    done

    "$SCRIPT_DIR/run_sharing_tests.sh"
    ;;
esac

# Tüm fail dosyalarını topla
echo ""
echo "========================================"
FAILED_LINES=()
for f in "$FAIL_DIR"/*.txt; do
  [ -f "$f" ] || continue
  while IFS= read -r line; do
    FAILED_LINES+=("$line")
  done < "$f"
done
rm -rf "$FAIL_DIR"

if [ ${#FAILED_LINES[@]} -eq 0 ]; then
  echo "✓ P1 Regression tamamlandı — tüm testler geçti."
else
  echo "❌ Başarısız testler:"
  for f in "${FAILED_LINES[@]}"; do echo "  - $f"; done
  exit 1
fi
