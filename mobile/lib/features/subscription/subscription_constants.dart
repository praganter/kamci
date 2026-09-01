import 'dart:io';

abstract final class SubscriptionConstants {
  static const String monthly = 'MONTHLY';
  static const String yearly = 'YEARLY';

  static const int freeDebtLimit = 5;
  static const int freeCategoryLimit = 2;

  static String get monthlyProductId => Platform.isIOS
      ? 'com.batuhanyetgin.kamci.premium.monthly'
      : 'premium_monthly';

  static String get yearlyProductId => Platform.isIOS
      ? 'com.batuhanyetgin.kamci.premium.yearly'
      : 'premium_yearly';

  static Set<String> get productIds => {monthlyProductId, yearlyProductId};

  /// Backend `/subscription/verify` için platform ayrımı (IOS | ANDROID).
  static String get platform => Platform.isIOS ? 'IOS' : 'ANDROID';

  /// Android'de `/subscription/verify` body'sinde zorunlu; iOS'ta null.
  static String? get androidPackageName =>
      Platform.isIOS ? null : 'com.batuhanyetgin.kamci';

  static String planForProductId(String productId) =>
      productId == monthlyProductId ? monthly : yearly;
}
