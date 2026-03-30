import 'dart:io';

abstract final class SubscriptionConstants {
  static const String monthly = 'MONTHLY';
  static const String yearly = 'YEARLY';

  static String get monthlyProductId => Platform.isIOS
      ? 'dev.batuhanyetgin.debttracker.premium.monthly'
      : 'premium_monthly';

  static String get yearlyProductId => Platform.isIOS
      ? 'dev.batuhanyetgin.debttracker.premium.yearly'
      : 'premium_yearly';

  static Set<String> get productIds => {monthlyProductId, yearlyProductId};

  static String planForProductId(String productId) =>
      productId == monthlyProductId ? monthly : yearly;
}
