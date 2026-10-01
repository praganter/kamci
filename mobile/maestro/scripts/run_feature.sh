#!/bin/bash
# run_feature.sh — tag'e göre seçili Maestro flow'larını tek tek koşturur.
# Tag'ler her flow'un header'ında: `tags: [<klasör>, smoke?, manual?, multi-device?]`
#
# Kullanım:
#   ./run_feature.sh debt                 # sadece debt flow'ları (Android)
#   ./run_feature.sh debt income ios      # birden fazla tag, iOS
#   ./run_feature.sh smoke                # P0 smoke seti
#   ./run_feature.sh all                  # manual + multi-device hariç hepsi
#   ./run_feature.sh flows/debt/pay_full.yaml   # tek dosya
#
# Flow'lar tek tek koşar (paylaşılan backend hesabı → paralel/toplu koşum izolasyonu bozar).
# sharing (multi-device) için: run_sharing_tests.sh · deeplink (manual): gerçek token gerekir.
set -o pipefail
export PATH="$HOME/.maestro/bin:$PATH"

MAESTRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$MAESTRO_DIR"

PLATFORM=android
TAGS=()
for arg in "$@"; do
  case "$arg" in
    android|ios) PLATFORM="$arg" ;;
    *) TAGS+=("$arg") ;;
  esac
done
[ ${#TAGS[@]} -eq 0 ] && { sed -n '2,13p' "$0"; exit 1; }

case "$PLATFORM" in
  ios)     DEVICE="15C5AAE6-5890-441F-9FA1-410AE0090B92" ;;
  android) DEVICE="emulator-5554" ;;
esac

[ -f .env ] || scripts/setup_test_accounts.sh
set -a; source .env; set +a

# Tag → dosya listesi (header'daki `tags:` satırından)
FILES=()
for tag in "${TAGS[@]}"; do
  if [ -f "$tag" ]; then FILES+=("$tag"); continue; fi
  if [ "$tag" = all ]; then
    mapfile -t found < <(grep -L -E '^tags:.*\b(manual|multi-device)\b' flows/*/*.yaml)
  else
    mapfile -t found < <(grep -l -E "^tags:.*[[ ,]$tag[],]" flows/*/*.yaml)
  fi
  [ ${#found[@]} -eq 0 ] && echo "⚠️  '$tag' tag'li flow yok"
  FILES+=("${found[@]}")
done
mapfile -t FILES < <(printf '%s\n' "${FILES[@]}" | awk 'NF && !seen[$0]++')
[ ${#FILES[@]} -eq 0 ] && exit 1

echo "▶ $PLATFORM ($DEVICE) — ${#FILES[@]} flow: ${TAGS[*]}"
FAILED=()
for f in "${FILES[@]}"; do
  echo "→ $f"
  maestro test --device "$DEVICE" --config config.yaml \
    -e USER_A_EMAIL="$USER_A_EMAIL" -e USER_A_PASSWORD="$USER_A_PASSWORD" \
    -e USER_B_EMAIL="$USER_B_EMAIL" -e USER_B_PASSWORD="$USER_B_PASSWORD" \
    "$f" || FAILED+=("$f")
done

echo ""
echo "✓ Geçen: $(( ${#FILES[@]} - ${#FAILED[@]} )) / ${#FILES[@]}"
if [ ${#FAILED[@]} -gt 0 ]; then
  echo "❌ Başarısız:"; printf '  - %s\n' "${FAILED[@]}"; exit 1
fi
