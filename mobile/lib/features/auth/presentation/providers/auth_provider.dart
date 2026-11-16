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

  // Reads a single Keychain key, returning null on timeout OR PlatformException.
  // After `xcrun simctl keychain reset` the securityd daemon is killed; reads
  // may hang (handled by timeout) or throw errSecInteractionNotAllowed
  // (handled by catch).  Either way we treat it as "no token".
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

    // Guard: login() may have completed while we were awaiting Keychain reads.
    // Don't overwrite an authenticated state with unauthenticated.
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

    // Access token expired — attempt refresh
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
      // Offline — keep the user signed in with stale tokens; API calls handle 401s
      state = AuthState.authenticated(tokens);
    } on UnauthorizedException {
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    } catch (_) {
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    }
  }

  // Writes a single Keychain key with a 5-second timeout + exception swallow.
  // After `clearKeychain` (E2E tests) securityd may still be restarting; each
  // write is best-effort — if it times out the session still works via the
  // in-memory token, but the next cold start will require re-login.
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

  /// Called by AuthInterceptor after a successful silent refresh so that
  /// subsequent requests immediately use the new access token from memory.
  /// Synchronous — no Keychain writes — so it never throws or blocks.
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
    } on NetworkException {
      // Offline — keep current state; tokens remain valid locally
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
    // Re-entrancy guard: unregisterToken() 401 alırsa AuthInterceptor refresh
    // dener, başarısız olursa tekrar logout() çağırır. Guard olmadan bu
    // logout → DELETE device-token → 401 → logout döngüsü sonsuza kadar sürer
    // ve kullanıcının yeni login'ini de siler (Maestro P0, 2026-09-26).
    // Zaten çıkış yapılmışsa (logout sonrası gecikmiş bir isteğin 401'i
    // interceptor'dan buraya düşer) token'sız DELETE atmaya gerek yok.
    if (_loggingOut || state is AuthStateUnauthenticated) return;
    _loggingOut = true;
    try {
      final storage = ref.read(secureStorageServiceProvider);
      // Cihaz token'ı, oturum token'ları hâlâ geçerliyken silinmeli — sunucu
      // logout'u refresh token'ı blacklist'ledikten sonra 401'de refresh
      // yapılamaz.
      await ref.read(notificationServiceProvider).unregisterToken();
      // Best-effort: sunucuda refresh token'ı blacklist'le. Başarısız olsa bile
      // (offline / token zaten geçersiz) yerel logout devam eder.
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

  /// Hesabı sunucuda kalıcı olarak siler, sonra yerel oturumu temizler.
  /// Sunucu silmesi başarısız olursa hata fırlatır ve oturum açık kalır —
  /// kullanıcı hesabının silindiğini sanıp çıkmamalı.
  Future<void> deleteAccount() async {
    // Cihaz token kaydı access token geçerliyken silinmeli (logout ile aynı
    // sıra). Best-effort: hesap zaten silinecek.
    try {
      await ref.read(notificationServiceProvider).unregisterToken();
    } catch (_) {}
    await ref.read(authRepositoryProvider).deleteAccount();
    await ref.read(secureStorageServiceProvider).clearAll();
    state = const AuthState.unauthenticated();
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
