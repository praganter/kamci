import 'dart:io';

abstract final class SubscriptionConstants {
  static const int freeDebtLimit = 5;
  static const int freeCategoryLimit = 2;

  /// RevenueCat dashboard'daki entitlement identifier'ı.
  static const String entitlementId = 'premium';

  /// RevenueCat public SDK key'leri — build'de verilir:
  /// `--dart-define=RC_API_KEY_IOS=appl_... --dart-define=RC_API_KEY_ANDROID=goog_...`
  /// Boşsa RevenueCat yapılandırılmaz, premium ekranı "mağaza yok" gösterir.
  static String get revenueCatApiKey => Platform.isIOS
      ? const String.fromEnvironment('RC_API_KEY_IOS')
      : const String.fromEnvironment('RC_API_KEY_ANDROID');
}
