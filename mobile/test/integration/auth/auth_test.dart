import 'package:dio/dio.dart';
import 'package:kamci/core/network/api_exception.dart';
import 'package:kamci/features/auth/data/datasources/auth_remote_datasource.dart';
import 'package:kamci/features/auth/data/models/login_request.dart';
import 'package:kamci/features/auth/data/models/register_request.dart';
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
          markTestSkipped('register rate-limited (429) — ortam, kod değil');
        } on ApiException catch (e) {
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
