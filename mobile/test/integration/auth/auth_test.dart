import 'package:dio/dio.dart';
import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:dt_mobile/features/auth/data/datasources/auth_remote_datasource.dart';
import 'package:dt_mobile/features/auth/data/models/login_request.dart';
import 'package:dt_mobile/features/auth/data/models/register_request.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/test_http_client.dart';

void main() {
  late AuthRemoteDatasource ds;

  setUpAll(() async {
    await TestHttpClient.setUp();
    ds = AuthRemoteDatasource(TestHttpClient.plain, TestHttpClient.authed);
  });

  group('register', () {
    test('yeni kullanıcı oluşturur ve token döner', () async {
      final ts = DateTime.now().millisecondsSinceEpoch;
      final result = await ds.register(
        RegisterRequest(email: 'new_$ts@testmail.com', password: testPassword),
      );
      expect(result.accessToken, isNotEmpty);
      expect(result.refreshToken, isNotEmpty);
    });

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
        LoginRequest(email: TestHttpClient.emailA, password: testPassword),
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
          baseUrl: 'http://198.51.100.10:30080',
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
