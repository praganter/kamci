import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:dt_mobile/core/services/notification_service.dart';
import 'package:dt_mobile/core/storage/secure_storage_service.dart';
import 'package:dt_mobile/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:dt_mobile/features/auth/domain/entities/auth_tokens.dart';
import 'package:dt_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:dt_mobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:dt_mobile/features/auth/presentation/providers/auth_state.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fakes.dart';

class _FakeAuthRepo extends Fake implements AuthRepository {
  Object? loginError;
  late AuthTokens tokens;

  @override
  Future<AuthTokens> login(String email, String password) async {
    if (loginError != null) throw loginError!;
    return tokens;
  }

  @override
  Future<void> logout(String refreshToken) async {}
}

void main() {
  late _FakeAuthRepo repo;
  late FakeStorage storage;
  late ProviderContainer c;

  setUp(() async {
    repo = _FakeAuthRepo()
      ..tokens = AuthTokens(
        accessToken: fakeJwt(sub: 'u-42', premium: true),
        refreshToken: 'refresh-1',
      );
    storage = FakeStorage();
    c = ProviderContainer(
      overrides: [
        authRepositoryProvider.overrideWithValue(repo),
        secureStorageServiceProvider.overrideWithValue(storage),
        notificationServiceProvider.overrideWithValue(FakeNotificationService()),
      ],
    );
    addTearDown(c.dispose);
    // Let the initial Keychain probe settle (no stored tokens → unauthenticated).
    c.read(authNotifierProvider);
    await Future<void>.delayed(Duration.zero);
  });

  test('starts unauthenticated when nothing is stored', () {
    expect(c.read(authNotifierProvider), isA<AuthStateUnauthenticated>());
  });

  test('login success → authenticated, tokens + email persisted', () async {
    await c.read(authNotifierProvider.notifier).login('a@example.com', 'pw');

    final state = c.read(authNotifierProvider) as AuthStateAuthenticated;
    expect(state.tokens.userId, 'u-42');
    expect(state.tokens.isPremium, isTrue);
    expect(storage.values['refresh'], 'refresh-1');
    expect(storage.values['email'], 'a@example.com');
  });

  test('wrong credentials → error state carrying the backend message', () async {
    repo.loginError = const UnauthorizedException('Bad credentials');

    await c.read(authNotifierProvider.notifier).login('a@example.com', 'x');

    final state = c.read(authNotifierProvider);
    expect(state, isA<AuthStateError>());
    expect((state as AuthStateError).message, 'Bad credentials');
    expect(storage.values, isEmpty);
  });

  test('unexpected failure → generic error, never throws to the UI', () async {
    repo.loginError = StateError('boom');

    await c.read(authNotifierProvider.notifier).login('a@example.com', 'x');

    expect(c.read(authNotifierProvider), isA<AuthStateError>());
  });

  test('logout clears stored session and ends unauthenticated', () async {
    await c.read(authNotifierProvider.notifier).login('a@example.com', 'pw');
    await c.read(authNotifierProvider.notifier).logout();

    expect(c.read(authNotifierProvider), isA<AuthStateUnauthenticated>());
    expect(storage.values['access'], isNull);
  });

  group('AuthTokens JWT claims', () {
    test('expired token is detected, future one is not', () {
      expect(
        AuthTokens(
          accessToken: fakeJwt(exp: 1),
          refreshToken: 'r',
        ).isAccessTokenExpired,
        isTrue,
      );
      expect(
        AuthTokens(
          accessToken: fakeJwt(),
          refreshToken: 'r',
        ).isAccessTokenExpired,
        isFalse,
      );
    });

    test('garbage token fails closed (expired, not premium, no user)', () {
      const t = AuthTokens(accessToken: 'not-a-jwt', refreshToken: 'r');
      expect(t.isAccessTokenExpired, isTrue);
      expect(t.isPremium, isFalse);
      expect(t.userId, isNull);
    });
  });
}
