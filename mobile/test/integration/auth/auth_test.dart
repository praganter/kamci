import 'package:dio/dio.dart';
import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:dt_mobile/features/auth/data/datasources/auth_remote_datasource.dart';
import 'package:dt_mobile/features/auth/data/models/login_request.dart';
import 'package:dt_mobile/features/auth/data/models/register_request.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/test_credentials.dart';
import '../helpers/test_http_client.dart';

void main() {
  if (!TestAccounts.hasA) {
    test('auth integration (skipped)', () {}, skip: TestAccounts.missingMessage);
    return;
  }

  late AuthRemoteDatasource ds;

  setUpAll(() async {
    await TestHttpClient.setUpAOnly();
    ds = AuthRemoteDatasource(TestHttpClient.plain, TestHttpClient.authed);
  });

  group('register', () {
    // Sabit "probe" e-posta: ilk çalıştırmada kullanıcı oluşturulur, sonraki
    // koşularda backend 409 döner. Her iki durum da register akışının ayakta
    // olduğunu doğrular — HER KOŞUDA yeni kullanıcı yaratmaz (rate-limit dostu).
    // `register` tag'i ile: gateway /auth/register kısıtlıysa
    // `--exclude-tags register` ile atlanabilir.
    test(
      'register akışı token döndürür (ilk koşu) ya da zaten kayıtlı (409)',
      () async {
        const probeEmail = 'integration_register_probe@testmail.com';
        try {
          final result = await ds.register(
            RegisterRequest(email: probeEmail, password: testPassword),
          );
          expect(result.accessToken, isNotEmpty);
          expect(result.refreshToken, isNotEmpty);
        } on RateLimitException {
          // Gateway /auth/register saturasyonu — akışla ilgisiz ortam durumu.
          markTestSkipped('register rate-limited (429) — ortam, kod değil');
        } on ApiException catch (e) {
          // Zaten kayıtlı — beklenen kararlı durum (409 → BusinessException).
          expect(e, isA<BusinessException>());
        }
      },
      tags: ['register'],
    );

    test('aynı e-posta ile tekrar register hata fırlatır', () async {
      expect(
        () => ds.register(
          RegisterRequest(email: TestHttpClient.emailA, password: testPassword),
        ),
        throwsA(isA<ApiException>()),
      );
    });
  });

  group('login', () {
    test('geçerli kimlik bilgileriyle token döner', () async {
      final result = await ds.login(
        LoginRequest(
          email: TestHttpClient.emailA,
          password: TestAccounts.passwordA,
        ),
      );
      expect(result.accessToken, isNotEmpty);
      expect(result.refreshToken, isNotEmpty);
    });

    test('yanlış şifreyle UnauthorizedException fırlar', () async {
      expect(
        () => ds.login(
          LoginRequest(email: TestHttpClient.emailA, password: 'yanlis_sifre_!'),
        ),
        throwsA(isA<UnauthorizedException>()),
      );
    });

    test('var olmayan e-posta ile NotFoundException fırlar', () async {
      // Backend var olmayan kullanıcı için 404 döner
      expect(
        () => ds.login(
          LoginRequest(email: 'hicyok_${DateTime.now().millisecondsSinceEpoch}@testmail.com', password: 'herhangi'),
        ),
        throwsA(isA<NotFoundException>()),
      );
    });
  });

  group('userDetail', () {
    test('geçerli token ile kullanıcı bilgisi döner', () async {
      final detail = await ds.userDetail();
      expect(detail.email, equals(TestHttpClient.emailA));
    });

    test('geçersiz token ile ApiException fırlar', () async {
      // Backend geçersiz JWT için 401 ya da 403 döner; her ikisi de ApiException
      final fakeDio = Dio(
        BaseOptions(
          baseUrl: const String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080'),
          headers: {
            'Authorization': 'Bearer sahte.token.bilgi',
            'Content-Type': 'application/json',
          },
        ),
      );
      final invalidDs = AuthRemoteDatasource(TestHttpClient.plain, fakeDio);
      expect(
        () => invalidDs.userDetail(),
        throwsA(isA<ApiException>()),
      );
    });
  });

  group('refreshToken', () {
    test('geçerli refresh token ile yeni access token alınır', () async {
      // setUp sırasında login'den alınan refresh token kullanılır
      final refreshed = await ds.refreshToken(TestHttpClient.refreshTokenA);
      expect(refreshed.accessToken, isNotEmpty);
    });

    test('geçersiz refresh token ile ApiException fırlar', () async {
      expect(
        () => ds.refreshToken('sahte.refresh.token'),
        throwsA(isA<ApiException>()),
      );
    });
  });
}
