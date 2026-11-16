#!/bin/bash
# Sharing testleri — 2 cihaz orkestrasyonu (A önce, B sonra)
# Kullanım:
#   ./run_sharing_tests.sh           # Android (varsayılan)
#   ./run_sharing_tests.sh android   # Android emülatör
#   ./run_sharing_tests.sh ios       # iOS Simulator (iPhone 16 + iPhone 16 Plus)
set -e
export PATH="$HOME/.maestro/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAESTRO_DIR="$SCRIPT_DIR/.."
FLOWS="$MAESTRO_DIR/flows/sharing"
PLATFORM="${1:-android}"

set -a; source "$MAESTRO_DIR/.env"; set +a
ENV_FLAGS="--env USER_A_EMAIL=$USER_A_EMAIL --env USER_A_PASSWORD=$USER_A_PASSWORD --env USER_B_EMAIL=$USER_B_EMAIL --env USER_B_PASSWORD=$USER_B_PASSWORD"

case "$PLATFORM" in
  ios)
    DEVICE_A="15C5AAE6-5890-441F-9FA1-410AE0090B92"   # iPhone 16  (User A)
    DEVICE_B="EF05C5CA-E623-42B8-A652-B0917D2A94A2"   # iPhone 16 Plus (User B)
    for DEVICE in "$DEVICE_A" "$DEVICE_B"; do
      if ! xcrun simctl list devices booted | grep -q "$DEVICE"; then
        echo "❌ Simulatör bulunamadı: $DEVICE — 'xcrun simctl boot <UDID>' çalıştırın."
        exit 1
      fi
    done
    ;;
  android|*)
    DEVICE_A="emulator-5554"
    DEVICE_B="emulator-5556"
    for DEVICE in "$DEVICE_A" "$DEVICE_B"; do
      if ! adb -s "$DEVICE" get-state &>/dev/null; then
        echo "❌ Cihaz bulunamadı: $DEVICE — 'adb devices' ile mevcut cihazları listeleyin."
        exit 1
      fi
    done
    ;;
esac

run_pair() {
  local FLOW_A=$1
  local FLOW_B=$2
  echo ""
  echo "--- $FLOW_A ---"
  maestro test --device "$DEVICE_A" $ENV_FLAGS "$FLOWS/$FLOW_A" && \
  maestro test --device "$DEVICE_B" $ENV_FLAGS "$FLOWS/$FLOW_B" || \
  echo "❌ FAILED: $FLOW_A / $FLOW_B"
}

echo "========================================"
echo "Sharing & Permissions Tests (2 Cihaz — $PLATFORM)"
echo "  Device A (User A): $DEVICE_A"
echo "  Device B (User B): $DEVICE_B"
echo "========================================"

run_pair "device_a_share_read.yaml"  "device_b_verify_read.yaml"
run_pair "device_a_share_write.yaml" "device_b_verify_write.yaml"
run_pair "device_a_share_edit.yaml"  "device_b_verify_edit.yaml"
run_pair "device_a_share_admin.yaml" "device_b_verify_admin.yaml"

echo ""
echo "--- Üye çıkarma ---"
maestro test --device "$DEVICE_A" $ENV_FLAGS "$FLOWS/remove_member.yaml" && \
maestro test --device "$DEVICE_B" $ENV_FLAGS "$FLOWS/device_b_verify_removed.yaml"

echo ""
echo "--- Toplu paylaşım ---"
maestro test --device "$DEVICE_A" $ENV_FLAGS "$FLOWS/bulk_share.yaml"

echo ""
echo "✓ Sharing testleri tamamlandı: $PLATFORM"
