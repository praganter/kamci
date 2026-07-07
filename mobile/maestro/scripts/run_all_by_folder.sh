#!/bin/bash
# run_all_by_folder.sh — Maestro testlerini klasöre göre sırayla koştur.
# Her klasörü tek bir driver session'ında çalıştırır (XCTest startup overhead'i önler).
# Her klasör bittikten sonra rapor gösterir ve kullanıcıdan onay bekler.
#
# Kullanım:
#   ./run_all_by_folder.sh           # iOS (varsayılan)
#   ./run_all_by_folder.sh android   # Android

set -o pipefail
export PATH="$HOME/.maestro/bin:$PATH"
# XCTest driver başlama süresi — varsayılan 60s yerine 3dk
export MAESTRO_DRIVER_STARTUP_TIMEOUT=180000

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAESTRO_DIR="$SCRIPT_DIR/.."
FLOWS="$MAESTRO_DIR/flows"

# Renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Platform / Cihaz
PLATFORM="${1:-ios}"
IOS_DEVICE="15C5AAE6-5890-441F-9FA1-410AE0090B92"
ANDROID_DEVICE="emulator-5554"

case "$PLATFORM" in
  ios)     DEVICE="$IOS_DEVICE" ;;
  android) DEVICE="$ANDROID_DEVICE" ;;
  *)       echo "Kullanım: $0 [ios|android]"; exit 1 ;;
esac

# .env yükle
if [ ! -f "$MAESTRO_DIR/.env" ]; then
  echo "❌ .env bulunamadı: $MAESTRO_DIR/.env"; exit 1
fi
set -a; source "$MAESTRO_DIR/.env"; set +a

# Koşulacak klasörler — mantıksal sıra
FOLDERS=("auth" "category" "debt" "charts" "premium" "settings" "income" "notifications" "edge_cases")
# Atlanan: deeplink (gerçek token), sharing (çift cihaz)

TOTAL_PASSED=0
TOTAL_FAILED=0
GRAND_FAILED=()

TMP_XML="/tmp/maestro_junit_$$.xml"
trap 'rm -f "$TMP_XML"' EXIT

# ─── Başlık ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║   Maestro E2E — Klasör Bazlı Runner             ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════════════════╝${NC}"
echo -e "  Platform : ${CYAN}$PLATFORM${NC}  ($DEVICE)"
echo -e "  Klasörler: ${#FOLDERS[@]}  (atlanan: deeplink, sharing)"
echo ""

# ─── Cihaz kontrolü ───────────────────────────────────────────────────────────
if [ "$PLATFORM" = "ios" ]; then
  if ! xcrun simctl list devices booted 2>/dev/null | grep -q "$IOS_DEVICE"; then
    echo -e "${RED}❌ iPhone 16 simulatörü kapalı (UDID: $IOS_DEVICE). Lütfen açın.${NC}"
    exit 1
  fi
else
  if ! adb -s "$ANDROID_DEVICE" get-state &>/dev/null; then
    echo -e "${RED}❌ Android emülatör bulunamadı: $ANDROID_DEVICE${NC}"
    exit 1
  fi
fi

# ─── JUnit XML parse yardımcı fonksiyonu ──────────────────────────────────────
# $1 = xml dosyası  → stdout'a "PASS|test_name" veya "FAIL|test_name|mesaj" basar
parse_junit() {
  local xml="$1"
  # Her <testcase> satırını işle
  python3 - "$xml" <<'PYEOF'
import sys, xml.etree.ElementTree as ET
tree = ET.parse(sys.argv[1])
root = tree.getroot()
# testsuites > testsuite > testcase  veya  testsuite > testcase
for tc in root.iter('testcase'):
    name = tc.get('name', 'unknown')
    failure = tc.find('failure')
    error   = tc.find('error')
    skipped = tc.find('skipped')
    if failure is not None:
        msg = (failure.get('message') or failure.text or '').split('\n')[0][:80]
        print(f"FAIL|{name}|{msg}")
    elif error is not None:
        msg = (error.get('message') or error.text or '').split('\n')[0][:80]
        print(f"FAIL|{name}|{msg}")
    elif skipped is not None:
        print(f"SKIP|{name}|")
    else:
        print(f"PASS|{name}|")
PYEOF
}

# ─── Klasör döngüsü ───────────────────────────────────────────────────────────
for FOLDER in "${FOLDERS[@]}"; do
  FOLDER_PATH="$FLOWS/$FOLDER"

  if [ ! -d "$FOLDER_PATH" ]; then
    echo -e "${YELLOW}⚠️  $FOLDER klasörü bulunamadı — atlandı${NC}"
    continue
  fi

  YAML_COUNT=$(find "$FOLDER_PATH" -maxdepth 1 -name "*.yaml" | wc -l | tr -d ' ')
  if [ "$YAML_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}⚠️  $FOLDER: hiç .yaml yok — atlandı${NC}"
    continue
  fi

  echo ""
  echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BOLD}  📁 KLASÖR: ${CYAN}$FOLDER${NC}${BOLD}  ($YAML_COUNT test) — sırayla koşturuluyor...${NC}"
  echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

  # ─── Her testi sırayla koştur (paralel çakışmayı önler) ──────────────────
  FOLDER_PASSED=0
  FOLDER_FAILED=0
  FOLDER_FAILED_LIST=()

  for YAML_FILE in "$FOLDER_PATH"/*.yaml; do
    TEST_NAME=$(basename "$YAML_FILE" .yaml)
    printf "  %-42s " "$TEST_NAME"

    maestro test \
      --device "$DEVICE" \
      --config "$MAESTRO_DIR/config.yaml" \
      --format JUNIT \
      --output "$TMP_XML" \
      -e USER_A_EMAIL="$USER_A_EMAIL" \
      -e USER_A_PASSWORD="$USER_A_PASSWORD" \
      -e USER_B_EMAIL="$USER_B_EMAIL" \
      -e USER_B_PASSWORD="$USER_B_PASSWORD" \
      "$YAML_FILE" >/dev/null 2>&1 || true

    if [ -f "$TMP_XML" ]; then
      RESULT=$(parse_junit "$TMP_XML" | head -1)
      STATUS=$(echo "$RESULT" | cut -d'|' -f1)
      MSG=$(echo "$RESULT" | cut -d'|' -f3)
      rm -f "$TMP_XML"
    else
      STATUS="FAIL"
      MSG="XML çıktısı oluşturulamadı"
    fi

    case "$STATUS" in
      PASS)
        echo -e "${GREEN}PASS${NC}"
        ((FOLDER_PASSED++)) || true
        ;;
      FAIL)
        echo -e "${RED}FAIL${NC}"
        [ -n "$MSG" ] && echo -e "    ${RED}↳ $MSG${NC}"
        ((FOLDER_FAILED++)) || true
        FOLDER_FAILED_LIST+=("$TEST_NAME")
        GRAND_FAILED+=("$FOLDER/$TEST_NAME")
        ;;
      SKIP)
        echo -e "${YELLOW}SKIP${NC}"
        ;;
    esac
  done

  # ─── Klasör raporu ─────────────────────────────────────────────────────────
  FOLDER_TOTAL=$((FOLDER_PASSED + FOLDER_FAILED))
  echo ""
  echo -e "${BOLD}  ┌─────────────────────────────────────────────────${NC}"
  echo -e "${BOLD}  │  📊 $FOLDER RAPORU${NC}"
  if [ "$FOLDER_FAILED" -eq 0 ]; then
    echo -e "${BOLD}  │  ${GREEN}Tümü geçti: $FOLDER_PASSED / $FOLDER_TOTAL${NC}"
  else
    echo -e "${BOLD}  │  ${GREEN}Geçen    : $FOLDER_PASSED / $FOLDER_TOTAL${NC}"
    echo -e "${BOLD}  │  ${RED}Başarısız: $FOLDER_FAILED / $FOLDER_TOTAL${NC}"
    echo -e "${BOLD}  │${NC}"
    echo -e "${BOLD}  │  Başarısız testler:${NC}"
    for T in "${FOLDER_FAILED_LIST[@]}"; do
      echo -e "${BOLD}  │    ${RED}• $T${NC}"
    done
  fi
  echo -e "${BOLD}  └─────────────────────────────────────────────────${NC}"

  ((TOTAL_PASSED += FOLDER_PASSED)) || true
  ((TOTAL_FAILED += FOLDER_FAILED)) || true

  echo ""
  echo -n "  ▶ Sonraki klasöre geçmek için Enter'a basın  (Ctrl+C = dur): "
  read -r _
done

# ─── Genel özet ───────────────────────────────────────────────────────────────
GRAND_TOTAL=$((TOTAL_PASSED + TOTAL_FAILED))
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║   GENEL ÖZET                                    ║${NC}"
echo -e "${BOLD}╟──────────────────────────────────────────────────╢${NC}"
printf "${BOLD}║  ${GREEN}Geçen   : %-36s${BOLD}║${NC}\n" "$TOTAL_PASSED / $GRAND_TOTAL"
printf "${BOLD}║  ${RED}Başarısız: %-35s${BOLD}║${NC}\n" "$TOTAL_FAILED / $GRAND_TOTAL"
if [ ${#GRAND_FAILED[@]} -gt 0 ]; then
  echo -e "${BOLD}╟──────────────────────────────────────────────────╢${NC}"
  echo -e "${BOLD}║  Başarısız:                                     ║${NC}"
  for T in "${GRAND_FAILED[@]}"; do
    printf "${BOLD}║    ${RED}• %-42s${BOLD}║${NC}\n" "$T"
  done
fi
echo -e "${BOLD}╠══════════════════════════════════════════════════╣${NC}"
printf "${BOLD}║  Atlanan: %-37s║${NC}\n" "deeplink/ (token), sharing/ (2 cihaz)"
echo -e "${BOLD}╚══════════════════════════════════════════════════╝${NC}"
echo ""
