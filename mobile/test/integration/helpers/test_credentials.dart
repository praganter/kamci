/// Share testleri için premium hesap bilgileri.
/// --dart-define=PREMIUM_EMAIL=... --dart-define=PREMIUM_PASSWORD=... ile sağlanır.
/// Verilmediğinde share testleri otomatik skip edilir.
abstract final class PremiumCredentials {
  static const email = String.fromEnvironment('PREMIUM_EMAIL');
  static const password = String.fromEnvironment('PREMIUM_PASSWORD');
  static bool get available => email.isNotEmpty && password.isNotEmpty;
}
