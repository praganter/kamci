import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;

abstract final class ApiConstants {
  static final String baseUrl = _fromDefine.isNotEmpty
      ? _fromDefine
      : (!kIsWeb && Platform.isAndroid
            ? 'http://10.0.2.2:8080'
            : 'http://localhost:8080');

  static const String _fromDefine = String.fromEnvironment('API_BASE_URL');

  static const String register = '/auth/register';
  static const String login = '/auth/login';
  static const String logout = '/auth/logout';
  static const String refreshToken = '/auth/refresh-token';
  static const String forgetPassword = '/auth/forget-password';
  static const String resetPassword = '/auth/reset-password';
  static const String changePassword = '/auth/change-password';
  static const String userDetail = '/auth/user-detail';
  static const String deleteAccount = '/auth/account';

  static const String householdInvite = '/auth/household/invite';
  static const String householdAccept = '/auth/household/accept';
  static const String householdDecline = '/auth/household/decline';
  static const String household = '/auth/household/';

  static const String debtSave = '/debt/save';
  static const String debtUpdate = '/debt/update';
  static const String debtPay = '/debt/pay';
  static const String debtBulkPay = '/debt/pay';
  static const String debtSkip = '/debt/skip';
  static const String debtSetPaidInstallments = '/debt';
  static const String debtDelete = '/debt/delete';
  static const String debtGetById = '/debt';
  static const String debtGet = '/debt/get';
  static const String debtSummary = '/debt/summary';

  static const String categorySave = '/category/save';
  static const String categoryUpdate = '/category/update';
  static const String categoryDelete = '/category/delete';
  static const String categoryGetAll = '/category/getAll';
  static const String categoryMemberAdd = '/category';
  static const String categoryMemberRemove = '/category';
  static const String categoryMemberBulk = '/category/members/bulk';
  static const String categoryMigrate = '/category';

  static const String incomeSave = '/income/save';
  static const String incomeHistory = '/income/history';
  static const String incomeCurrent = '/income/current';
  static const String incomeDelete = '/income/delete';

  static const String deviceToken = '/notification/device-token';
  static const String notificationList = '/notification/list';
  static const String notificationRead = '/notification';

  static const String exchangeRates = '/rates';
}
