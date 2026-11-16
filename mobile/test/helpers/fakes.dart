import 'dart:convert';

import 'package:dt_mobile/core/services/notification_service.dart';
import 'package:dt_mobile/core/storage/secure_storage_service.dart';
import 'package:flutter_test/flutter_test.dart';

String fakeJwt({String sub = 'user-1', bool premium = false, int? exp}) {
  String seg(Map<String, Object?> m) =>
      base64Url.encode(utf8.encode(jsonEncode(m))).replaceAll('=', '');
  return '${seg({'alg': 'none'})}.'
      '${seg({'sub': sub, 'premium': premium, 'exp': exp ?? 4102444800})}.sig';
}

class FakeStorage extends Fake implements SecureStorageService {
  final values = <String, String>{};

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    values['access'] = accessToken;
    values['refresh'] = refreshToken;
  }

  @override
  Future<void> saveEmail(String email) async => values['email'] = email;

  @override
  Future<void> saveUserId(String userId) async => values['userId'] = userId;

  @override
  Future<String?> getAccessToken() async => values['access'];

  @override
  Future<String?> getRefreshToken() async => values['refresh'];

  @override
  Future<String?> getEmail() async => values['email'];

  @override
  Future<String?> getUserId() async => values['userId'];

  @override
  Future<void> clearAll() async => values.clear();
}

class FakeNotificationService extends Fake implements NotificationService {
  @override
  Future<void> registerToken({String? email}) async {}

  @override
  Future<void> unregisterToken() async {}
}
