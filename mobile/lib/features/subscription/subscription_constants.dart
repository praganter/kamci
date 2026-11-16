import 'dart:io';

abstract final class SubscriptionConstants {
  static const int freeDebtLimit = 5;
  static const int freeCategoryLimit = 2;

  static const String entitlementId = 'premium';

  static String get revenueCatApiKey => Platform.isIOS
      ? const String.fromEnvironment('RC_API_KEY_IOS')
      : const String.fromEnvironment('RC_API_KEY_ANDROID');
}
