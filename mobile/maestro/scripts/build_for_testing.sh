#!/bin/bash
# Maestro test build — Flutter animasyonları 100x hızlı (timeDilation=0.01)
# Kullanım: ./build_for_testing.sh [ios|android]
set -e

PLATFORM="${1:-ios}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
IOS_DEVICE="15C5AAE6-5890-441F-9FA1-410AE0090B92"

cd "$PROJECT_DIR"

case "$PLATFORM" in
  ios)
    echo "→ Flutter iOS debug build (MAESTRO=true)..."
    flutter build ios --debug --dart-define=MAESTRO=true --simulator
    echo "→ Simulatöre yükleniyor..."
    xcrun simctl install "$IOS_DEVICE" build/ios/iphonesimulator/Runner.app
    echo "✓ Test build kuruldu. Şimdi testleri çalıştırabilirsin:"
    echo "  bash maestro/scripts/run_p1_regression.sh ios"
    ;;
  android)
    echo "→ Flutter Android debug build (MAESTRO=true)..."
    flutter build apk --debug --dart-define=MAESTRO=true
    echo "→ Emülatöre yükleniyor..."
    adb install -r build/app/outputs/flutter-apk/app-debug.apk
    echo "✓ Test build kuruldu."
    ;;
  *)
    echo "Kullanım: $0 [ios|android]"
    exit 1
    ;;
esac
