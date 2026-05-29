import 'dart:convert';

import 'package:dio/dio.dart';

const _baseUrl = 'http://198.51.100.10:30080';

/// Register edilen kullanıcıların ortak parolası
const testPassword = 'TestPass123!';

abstract final class TestHttpClient {
  /// Token'sız Dio — register / login / refresh-token endpoint'leri için
  static late Dio plain;

  /// Kullanıcı A'nın Dio'su (sahip)
  static late Dio authed;

  /// Kullanıcı B'nin Dio'su (üye olarak eklenecek)
  static late Dio authedB;

  /// Kullanıcı A'nın e-postası
  static late String emailA;

  /// Kullanıcı B'nin e-postası
  static late String emailB;

  /// Kullanıcı A'nın ID'si (JWT'den parse edilir)
  static late String userId;

  /// Kullanıcı B'nin ID'si
  static late String userIdB;

  /// Kullanıcı A'nın refresh token'ı (auth testleri için)
  static late String refreshTokenA;

  /// Her test dosyasının setUpAll'ında çağrılır.
  /// Timestamp'li benzersiz e-postalarla 2 yeni kullanıcı register + login eder.
  static Future<void> setUp() async {
    plain = _buildDio();

    final ts = DateTime.now().millisecondsSinceEpoch;
    emailA = 'test_a_$ts@testmail.com';
    emailB = 'test_b_$ts@testmail.com';

    final (dioA, tokenA, refreshA) = await _registerAndLogin(emailA);
    authed = dioA;
    userId = _parseUserId(tokenA);
    refreshTokenA = refreshA;

    final (dioB, tokenB, _) = await _registerAndLogin(emailB);
    authedB = dioB;
    userIdB = _parseUserId(tokenB);
  }

  static Future<(Dio, String, String)> _registerAndLogin(
    String email, {
    int retries = 3,
  }) async {
    for (var attempt = 0; attempt <= retries; attempt++) {
      try {
        // Register returns tokens directly — no separate login call needed.
        final resp = await plain.post(
          '/auth/register',
          data: {'email': email, 'password': testPassword, 'premium': true},
        );
        final token = resp.data['token'] as String;
        final refresh = resp.data['refreshToken'] as String;
        final dio = _buildDio()
          ..options.headers['Authorization'] = 'Bearer $token';
        return (dio, token, refresh);
      } on DioException catch (e) {
        if (e.response?.statusCode == 429 && attempt < retries) {
          await Future<void>.delayed(Duration(seconds: 5 * (attempt + 1)));
          continue;
        }
        rethrow;
      }
    }
    throw StateError('_registerAndLogin: unreachable');
  }

  static Dio _buildDio() => Dio(
    BaseOptions(
      baseUrl: _baseUrl,
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Accept-Language': 'tr',
      },
    ),
  );

  static String _parseUserId(String token) {
    final parts = token.split('.');
    if (parts.length != 3) throw Exception('Geçersiz JWT formatı');
    final payload = base64Url.normalize(parts[1]);
    final decoded = utf8.decode(base64Url.decode(payload));
    final json = jsonDecode(decoded) as Map<String, dynamic>;
    final id =
        json['sub'] as String? ??
        json['id'] as String? ??
        json['userId'] as String?;
    if (id == null) throw Exception('JWT payload\'ında userId bulunamadı: $json');
    return id;
  }
}
