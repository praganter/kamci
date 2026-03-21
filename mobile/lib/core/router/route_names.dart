abstract final class RouteNames {
  static const String splash = 'splash';
  static const String login = 'login';
  static const String register = 'register';
  static const String forgotPassword = 'forgot-password';
  static const String resetPassword = 'reset-password';

  static const String home = 'home';
  static const String debts = 'debts';
  static const String debtNew = 'debt-new';
  static const String debtDetail = 'debt-detail';
  static const String debtEdit = 'debt-edit';

  static const String categories = 'categories';
  static const String categoryNew = 'category-new';
  static const String categoryDetail = 'category-detail';
  static const String categoryEdit = 'category-edit';

  static const String settings = 'settings';
  static const String changePassword = 'change-password';
}

abstract final class RoutePaths {
  static const String splash = '/splash';
  static const String login = '/login';
  static const String register = '/register';
  static const String forgotPassword = '/forgot-password';
  static const String resetPassword = '/reset-password';

  static const String home = '/home';
  static const String debts = '/debts';
  static const String debtNew = '/debts/new';
  static String debtDetail(String id) => '/debts/$id';
  static String debtEdit(String id) => '/debts/$id/edit';

  static const String categories = '/categories';
  static const String categoryNew = '/categories/new';
  static String categoryDetail(String id) => '/categories/$id';
  static String categoryEdit(String id) => '/categories/$id/edit';

  static const String settings = '/settings';
  static const String changePassword = '/change-password';
}
