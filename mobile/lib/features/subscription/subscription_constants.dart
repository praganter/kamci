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

  static String planForProductId(String productId) =>
      productId == monthlyProductId ? monthly : yearly;
}
