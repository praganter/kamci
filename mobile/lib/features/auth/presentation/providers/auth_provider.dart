import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/entities/auth_tokens.dart';
import '../../domain/entities/user_detail.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/services/notification_service.dart';
import '../../../../core/storage/secure_storage_service.dart';
import 'auth_state.dart';

part 'auth_provider.g.dart';

@Riverpod(keepAlive: true)
class AuthNotifier extends _$AuthNotifier {
  @override
  AuthState build() {
    _checkAuthState();
    return const AuthState.initial();
  }

  static Future<String?> _safeRead(Future<String?> read) async {
    try {
      return await read.timeout(
        const Duration(seconds: 3),
        onTimeout: () => null,
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> _checkAuthState() async {
    state = const AuthState.loading();

    final storage = ref.read(secureStorageServiceProvider);
    final accessToken = await _safeRead(storage.getAccessToken());
    final refreshToken = await _safeRead(storage.getRefreshToken());

    if (state is AuthStateAuthenticated) return;

    if (accessToken == null || refreshToken == null) {
      state = const AuthState.unauthenticated();
      return;
    }

    final tokens = AuthTokens(
      accessToken: accessToken,
      refreshToken: refreshToken,
    );

    if (!tokens.isAccessTokenExpired) {
      await _saveUserIdFromToken(tokens);
      state = AuthState.authenticated(tokens);
      final email = await storage.getEmail();
      ref.read(notificationServiceProvider).registerToken(email: email);
      return;
    }

    try {
      final repository = ref.read(authRepositoryProvider);
      final newTokens = await repository.refreshToken(refreshToken);
      await storage.saveTokens(
        accessToken: newTokens.accessToken,
        refreshToken: newTokens.refreshToken,
      );
      await _saveUserIdFromToken(newTokens);
      state = AuthState.authenticated(newTokens);
      final email = await storage.getEmail();
      ref.read(notificationServiceProvider).registerToken(email: email);
    } on NetworkException {
      state = AuthState.authenticated(tokens);
    } on UnauthorizedException {
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    } catch (_) {
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    }
  }

  static Future<void> _safeWrite(Future<void> write) async {
    try {
      await write.timeout(const Duration(seconds: 5), onTimeout: () {});
    } catch (_) {}
  }

  Future<void> login(String email, String password) async {
    state = const AuthState.loading();
    try {
      final repository = ref.read(authRepositoryProvider);
      final tokens = await repository.login(email, password);
      final storage = ref.read(secureStorageServiceProvider);
      await _safeWrite(storage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      ));
      await _safeWrite(storage.saveEmail(email));
      await _safeWrite(_saveUserIdFromToken(tokens));
      state = AuthState.authenticated(tokens);
      ref.read(notificationServiceProvider).registerToken(email: email).ignore();
    } on UnauthorizedException catch (e) {
      state = AuthState.error(e.message);
    } on ApiException catch (e) {
      state = AuthState.error(e.message);
    } catch (_) {
      state = const AuthState.error('An unexpected error occurred.');
    }
  }

  Future<void> register(String email, String password) async {
    state = const AuthState.loading();
    try {
      final repository = ref.read(authRepositoryProvider);
      final tokens = await repository.register(email, password);
      final storage = ref.read(secureStorageServiceProvider);
      await _safeWrite(storage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      ));
      await _safeWrite(storage.saveEmail(email));
      await _safeWrite(_saveUserIdFromToken(tokens));
      state = AuthState.authenticated(tokens);
      ref.read(notificationServiceProvider).registerToken(email: email).ignore();
    } on ApiException catch (e) {
      state = AuthState.error(e.message);
    } catch (_) {
      state = const AuthState.error('An unexpected error occurred.');
    }
  }

  void updateTokensFromInterceptor(AuthTokens tokens) {
    state = AuthState.authenticated(tokens);
  }

  Future<void> refreshTokens() async {
    final storage = ref.read(secureStorageServiceProvider);
    final refreshToken = await storage.getRefreshToken();
    if (refreshToken == null) return;
    try {
      final repository = ref.read(authRepositoryProvider);
      final newTokens = await repository.refreshToken(refreshToken);
      await storage.saveTokens(
        accessToken: newTokens.accessToken,
        refreshToken: newTokens.refreshToken,
      );
      await _saveUserIdFromToken(newTokens);
      state = AuthState.authenticated(newTokens);
    // ignore: empty_catches
    } on NetworkException {
    } on UnauthorizedException {
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    } catch (_) {
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    }
  }

  bool _loggingOut = false;

  Future<void> logout() async {
    if (_loggingOut || state is AuthStateUnauthenticated) return;
    _loggingOut = true;
    try {
      final storage = ref.read(secureStorageServiceProvider);
      await ref.read(notificationServiceProvider).unregisterToken();
      final refreshToken = await storage.getRefreshToken();
      if (refreshToken != null) {
        try {
          await ref.read(authRepositoryProvider).logout(refreshToken);
        } catch (_) {}
      }
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    } finally {
      _loggingOut = false;
    }
  }

  Future<void> deleteAccount() async {
    try {
      await ref.read(notificationServiceProvider).unregisterToken();
    } catch (_) {}
    await ref.read(authRepositoryProvider).deleteAccount();
    await ref.read(secureStorageServiceProvider).clearAll();
    state = const AuthState.unauthenticated();
  }

  Future<void> adoptTokens(AuthTokens tokens) async {
    await ref
        .read(secureStorageServiceProvider)
        .saveTokens(
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
        );
    await _saveUserIdFromToken(tokens);
    state = AuthState.authenticated(tokens);
  }

  Future<void> _saveUserIdFromToken(AuthTokens tokens) async {
    final userId = tokens.userId;
    if (userId != null) {
      await ref.read(secureStorageServiceProvider).saveUserId(userId);
    }
  }
}

@riverpod
Future<UserDetail> userDetail(Ref ref) =>
    ref.watch(authRepositoryProvider).userDetail();
