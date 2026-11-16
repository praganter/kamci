import 'dart:convert';

import 'package:dio/dio.dart';

import 'test_credentials.dart';

const _baseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8080',
);

const testPassword = 'TestPass123!';

abstract final class TestHttpClient {
  static late Dio plain;

  static late Dio authed;

  static late Dio authedB;

  static late String emailA;

  static late String emailB;

  static late String userId;

  static late String userIdB;

  static late String refreshTokenA;

  static Future<void> setUp() async {
    plain = _buildDio();
    emailA = TestAccounts.emailA;
    emailB = TestAccounts.emailB;

    final (dioA, tokenA, refreshA) =
        await _login(emailA, TestAccounts.passwordA);
    authed = dioA;
    userId = _parseUserId(tokenA);
    refreshTokenA = refreshA;

    final (dioB, tokenB, _) = await _login(emailB, TestAccounts.passwordB);
    authedB = dioB;
    userIdB = _parseUserId(tokenB);
  }

  static Future<void> setUpAOnly() async {
    plain = _buildDio();
    emailA = TestAccounts.emailA;

    final (dioA, tokenA, refreshA) =
        await _login(emailA, TestAccounts.passwordA);
    authed = dioA;
    userId = _parseUserId(tokenA);
    refreshTokenA = refreshA;
  }

  static Future<(Dio, String, String)> _login(
    String email,
    String password,
  ) async {
    final resp = await plain.post(
      '/auth/login',
      data: {'email': email, 'password': password},
    );
    final data = resp.data as Map<String, dynamic>;
    final token = data['token'] as String;
    final refresh = data['refreshToken'] as String? ?? '';
    final dio = _buildDio()..options.headers['Authorization'] = 'Bearer $token';
    return (dio, token, refresh);
  }

  static Future<void> cleanupResidualCategories() async {
    const knownNames = {
      'Test Kategori',
      'Liste Test Kategori',
      'Güncelleme Öncesi',
      'Güncelleme Sonrası',
      'Silinecek Kategori',
      '__Test Borç Kategorisi__',
    };
    try {
      final resp = await authed.get<dynamic>('/category/getAll');
      final list = (resp.data as List).cast<dynamic>();
      for (final raw in list) {
        final c = raw as Map<String, dynamic>;
        final name = c['name'] as String? ?? '';
        final id = (c['id'] ?? c['_id']) as String? ?? '';
        if (id.isNotEmpty && knownNames.contains(name)) {
          await authed
              .delete<dynamic>('/category/delete/$id')
              .catchError((_) => Response<dynamic>(
                    requestOptions: RequestOptions(path: ''),
                  ));
        }
      }
    } catch (_) {
    }
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
