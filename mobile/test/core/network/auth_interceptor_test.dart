import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:kamci/core/network/dio_client.dart';
import 'package:kamci/core/network/interceptors/auth_interceptor.dart';
import 'package:kamci/core/storage/secure_storage_service.dart';
import 'package:kamci/features/auth/domain/entities/auth_tokens.dart';
import 'package:kamci/features/auth/presentation/providers/auth_provider.dart';
import 'package:kamci/features/auth/presentation/providers/auth_state.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeApi implements HttpClientAdapter {
  _FakeApi(this.handler);

  final (int, Object?) Function(RequestOptions) handler;
  final calls = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    calls.add(options);
    final (status, body) = handler(options);
    return ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _FakeAuth extends AuthNotifier {
  _FakeAuth(this._initial);

  final AuthState _initial;
  int logoutCalls = 0;

  @override
  AuthState build() => _initial;

  @override
  Future<void> logout() async {
    logoutCalls++;
    state = const AuthState.unauthenticated();
  }
}

class _FakeStorage extends Fake implements SecureStorageService {
  String? savedAccess;
  String? savedRefresh;
  bool cleared = false;

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    savedAccess = accessToken;
    savedRefresh = refreshToken;
  }

  @override
  Future<void> clearAll() async => cleared = true;

  @override
  Future<String?> getAccessToken() async => null;

  @override
  Future<String?> getRefreshToken() async => null;
}

const _stale = AuthTokens(accessToken: 'old', refreshToken: 'r1');

({ProviderContainer c, Dio dio, _FakeApi api, _FakeAuth auth, _FakeStorage store})
_setup(
  (int, Object?) Function(RequestOptions) handler, {
  AuthState? state,
}) {
  final api = _FakeApi(handler);
  final auth = _FakeAuth(state ?? const AuthState.authenticated(_stale));
  final store = _FakeStorage();
  final dioProvider = Provider<Dio>((ref) {
    final d = Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter = api;
    d.interceptors.add(AuthInterceptor(ref));
    return d;
  });
  final c = ProviderContainer(
    overrides: [
      authNotifierProvider.overrideWith(() => auth),
      secureStorageServiceProvider.overrideWithValue(store),
      refreshDioProvider.overrideWith(
        (ref) => Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter = api,
      ),
    ],
  );
  addTearDown(c.dispose);
  return (c: c, dio: c.read(dioProvider), api: api, auth: auth, store: store);
}

String? _bearer(RequestOptions o) => o.headers['Authorization'] as String?;

void main() {
  test('attaches the in-memory access token as a Bearer header', () async {
    final t = _setup((_) => (200, {'ok': true}));
    await t.dio.get('/debt/get');
    expect(_bearer(t.api.calls.single), 'Bearer old');
  });

  test('401 → refresh → original request retried with the new token', () async {
    final t = _setup((o) {
      if (o.path == '/auth/refresh-token') {
        return (200, {'token': 'new', 'refreshToken': 'r2'});
      }
      return _bearer(o) == 'Bearer new' ? (200, {'ok': true}) : (401, {});
    });

    final res = await t.dio.get('/debt/get');

    expect(res.statusCode, 200);
    expect(t.api.calls.map((o) => o.path), [
      '/debt/get',
      '/auth/refresh-token',
      '/debt/get',
    ]);
    expect(_bearer(t.api.calls[1]), 'Bearer r1');
    expect(t.store.savedAccess, 'new');
    expect(t.store.savedRefresh, 'r2');
    final state = t.c.read(authNotifierProvider) as AuthStateAuthenticated;
    expect(state.tokens.accessToken, 'new');
    expect(t.auth.logoutCalls, 0);
  });

  test('expired-token error code (no 401 status needed) triggers refresh', () async {
    final t = _setup((o) {
      if (o.path == '/auth/refresh-token') {
        return (200, {'token': 'new', 'refreshToken': 'r2'});
      }
      return _bearer(o) == 'Bearer new'
          ? (200, {'ok': true})
          : (403, {'code': 'error.auth.token_expired'});
    });
    expect((await t.dio.get('/x')).statusCode, 200);
    expect(t.api.calls.where((o) => o.path == '/auth/refresh-token'), hasLength(1));
  });

  test('refresh rejected → session cleared and logout, error surfaces', () async {
    final t = _setup((o) => (401, {}));

    await expectLater(
      t.dio.get('/debt/get'),
      throwsA(isA<DioException>().having((e) => e.response?.statusCode, 'status', 401)),
    );
    expect(t.store.cleared, isTrue);
    expect(t.auth.logoutCalls, 1);
  });

  test('token_invalid logs out immediately without trying to refresh', () async {
    final t = _setup((_) => (401, {'code': 'error.auth.token_invalid'}));

    await expectLater(t.dio.get('/x'), throwsA(isA<DioException>()));
    expect(t.api.calls.where((o) => o.path == '/auth/refresh-token'), isEmpty);
    expect(t.auth.logoutCalls, 1);
  });

  test('wrong current password (401 on change-password) is not a session error', () async {
    final t = _setup((_) => (401, {'code': 'error.auth.bad_credentials'}));

    await expectLater(t.dio.post('/auth/change-password'), throwsA(isA<DioException>()));
    expect(t.api.calls, hasLength(1));
    expect(t.auth.logoutCalls, 0);
  });

  test('a retried request that still fails does not loop or log out', () async {
    final t = _setup((o) {
      if (o.path == '/auth/refresh-token') {
        return (200, {'token': 'new', 'refreshToken': 'r2'});
      }
      return (401, {});
    });

    await expectLater(t.dio.get('/x'), throwsA(isA<DioException>()));
    expect(t.api.calls, hasLength(3));
    expect(t.auth.logoutCalls, 0);
  });

  test('non-auth errors (500) pass through untouched', () async {
    final t = _setup((_) => (500, {'code': 'error.unexpected'}));
    await expectLater(t.dio.get('/x'), throwsA(isA<DioException>()));
    expect(t.api.calls, hasLength(1));
    expect(t.auth.logoutCalls, 0);
  });
}
