import 'dart:convert';

import 'package:dio/dio.dart';

import 'test_credentials.dart';

const _baseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8080',
);

/// `register` probe testinin (auth_test) yeni kullanıcı oluştururken
/// kullandığı parola. Kalıcı A/B hesaplarının parolaları [TestAccounts]'tan
/// gelir — bu sabit onlarla ilgisizdir.
const testPassword = 'TestPass123!';

/// Integration suite'leri için ortak HTTP istemcisi.
///
/// **Login-only:** kalıcı A/B hesaplarını ([TestAccounts]) login eder, her
/// koşuda yeni kullanıcı register ETMEZ (gateway `/auth/register` rate-limit'i).
abstract final class TestHttpClient {
  /// Token'sız Dio — login / register / refresh-token endpoint'leri için
  static late Dio plain;

  /// Kullanıcı A'nın Dio'su (premium sahip)
  static late Dio authed;

  /// Kullanıcı B'nin Dio'su (paylaşım üyesi)
  static late Dio authedB;

  /// Kullanıcı A'nın e-postası
  static late String emailA;

  /// Kullanıcı B'nin e-postası
  static late String emailB;

  /// Kullanıcı A'nın ID'si (JWT `sub` claim'inden parse edilir)
  static late String userId;

  /// Kullanıcı B'nin ID'si
  static late String userIdB;

  /// Kullanıcı A'nın refresh token'ı (auth testleri için)
  static late String refreshTokenA;

  /// Her test dosyasının `setUpAll`'ında çağrılır. Kalıcı A + B hesaplarını
  /// login eder. [TestAccounts.hasAB] false ise çağıran suite zaten
  /// top-level skip edilmiş olmalı.
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

  /// Yalnız A hesabını login eden suite'ler için (auth / category / debt).
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

  /// USER_A hesabında geçmiş (yarıda kalmış) koşulardan birikmiş test
  /// kategorisi kalıntılarını siler. Yalnız suite'lerin ürettiği BİLİNEN
  /// sabit isimleri hedefler — gerçek kullanıcı verisine / yabancı
  /// kategorilere dokunmaz. Best-effort: hata yutulur.
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
      // temizlik best-effort — asıl testleri bloklamasın
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
