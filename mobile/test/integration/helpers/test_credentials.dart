/// Kalıcı test hesapları — integration suite'leri artık her koşuda yeni
/// kullanıcı REGISTER ETMEZ; önceden var olan iki hesabı login eder.
///
/// `--dart-define` ile geçilir (bkz. `test/integration/run.sh`, `maestro/.env`
/// ile eşlenir):
///   TEST_A_EMAIL / TEST_A_PASSWORD  → premium "owner" hesabı (USER_A)
///   TEST_B_EMAIL / TEST_B_PASSWORD  → "member" hesabı (USER_B)
///
/// Verilmezse [available] false döner ve ilgili suite'ler top-level skip edilir.
abstract final class TestAccounts {
  static const emailA = String.fromEnvironment('TEST_A_EMAIL');
  static const passwordA = String.fromEnvironment('TEST_A_PASSWORD');
  static const emailB = String.fromEnvironment('TEST_B_EMAIL');
  static const passwordB = String.fromEnvironment('TEST_B_PASSWORD');

  /// A hesabı tek başına yeterli olan suite'ler için (auth login, category, debt).
  static bool get hasA => emailA.isNotEmpty && passwordA.isNotEmpty;

  /// A + B gereken suite'ler için (paylaşım).
  static bool get hasAB =>
      hasA && emailB.isNotEmpty && passwordB.isNotEmpty;

  static const missingMessage =
      'TEST_A_EMAIL / TEST_A_PASSWORD (+ paylaşım için TEST_B_*) --dart-define '
      'ile verilmeli — bkz. test/integration/run.sh';
}
