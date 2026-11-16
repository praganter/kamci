#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
MIN="${1:-0}"
flutter test test/core test/features test/shared --coverage >/dev/null
awk -v min="$MIN" '
{ sub(/\r$/, ""); gsub(/\\/, "/") }
/^SF:/ { skip = ($0 ~ /\.g\.dart$|\.freezed\.dart$|\/gen\//) }
/^DA:/ && !skip { split(substr($0, 4), a, ","); total++; if (a[2] > 0) hit++ }
END {
  pct = total ? 100 * hit / total : 0
  printf "lib line coverage (generated code excluded): %d/%d = %.1f%%\n", hit, total, pct
  if (pct < min) { printf "below required %s%%\n", min; exit 1 }
}' coverage/lcov.info
