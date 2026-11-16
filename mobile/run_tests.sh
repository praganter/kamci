#!/bin/bash
# ============================================================
# Integration Test Runner
# ============================================================
# Kullanım:
#   chmod +x run_tests.sh   (ilk seferinde)
#   ./run_tests.sh
# ============================================================

# ── Share testleri için premium hesap (isteğe bağlı) ──────
# Yoksa share testleri skip edilir, diğerleri çalışır.
PREMIUM_EMAIL="premium-user@example.com"
PREMIUM_PASSWORD="REPLACE_ME"

# ── Hangi testler çalışsın? ────────────────────────────────
# Hepsi için "test/integration/"
# Sadece kategori için "test/integration/category/category_crud_test.dart"
# Sadece debt için "test/integration/debt/"
# Sadece auth için "test/integration/auth/"
TARGET="test/integration/"

# ── Script ────────────────────────────────────────────────
DEFINES=""
if [ -n "$PREMIUM_EMAIL" ] && [ -n "$PREMIUM_PASSWORD" ]; then
  DEFINES="--dart-define=PREMIUM_EMAIL=$PREMIUM_EMAIL --dart-define=PREMIUM_PASSWORD=$PREMIUM_PASSWORD"
  echo "✓ Premium hesap bulundu — share testleri dahil çalışacak"
else
  echo "⚠  Premium hesap yok — share testleri skip edilecek"
  echo "   (PREMIUM_EMAIL ve PREMIUM_PASSWORD'ü script içinde doldur)"
fi

echo ""
echo "▶  flutter test $TARGET"
echo "────────────────────────────────────────────────────────"

flutter test "$TARGET" --reporter=expanded --concurrency=1 $DEFINES
