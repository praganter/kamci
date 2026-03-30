abstract final class ApiConstants {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  static const String register = '/auth/register';
  static const String login = '/auth/login';
  static const String refreshToken = '/auth/refresh-token';
  static const String forgetPassword = '/auth/forget-password';
  static const String resetPassword = '/auth/reset-password';
  static const String changePassword = '/auth/change-password';
  static const String userDetail = '/auth/user-detail';

  static const String debtSave = '/debt/save';
  static const String debtUpdate = '/debt/update';
  static const String debtPay = '/debt/pay';
  static const String debtDelete = '/debt/delete';
  static const String debtGetById = '/debt';
  static const String debtGet = '/debt/get';
  static const String categorySave = '/category/save';
  static const String categoryUpdate = '/category/update';
  static const String categoryDelete = '/category/delete';
  static const String categoryGetAll = '/category/getAll';
  static const String categoryMemberBulk = '/category/members/bulk';

  static const String subscriptionVerify = '/subscription/verify';
  static const String subscriptionStatus = '/subscription/status';
}
