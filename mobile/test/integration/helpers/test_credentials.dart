abstract final class TestAccounts {
  static const emailA = String.fromEnvironment('TEST_A_EMAIL');
  static const passwordA = String.fromEnvironment('TEST_A_PASSWORD');
  static const emailB = String.fromEnvironment('TEST_B_EMAIL');
  static const passwordB = String.fromEnvironment('TEST_B_PASSWORD');

  static bool get hasA => emailA.isNotEmpty && passwordA.isNotEmpty;

  static bool get hasAB =>
      hasA && emailB.isNotEmpty && passwordB.isNotEmpty;

  static const missingMessage =
      'TEST_A_EMAIL / TEST_A_PASSWORD (+ paylaşım için TEST_B_*) --dart-define '
      'ile verilmeli — bkz. test/integration/run.sh';
}
