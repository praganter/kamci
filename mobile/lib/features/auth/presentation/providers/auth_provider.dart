import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/entities/auth_tokens.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/storage/secure_storage_service.dart';
import 'auth_state.dart';

part 'auth_provider.g.dart';

@riverpod
class AuthNotifier extends _$AuthNotifier {
  @override
  AuthState build() {
    _checkAuthState();
    return const AuthState.initial();
  }

  Future<void> _checkAuthState() async {
    state = const AuthState.loading();
    try {
      final storage = ref.read(secureStorageServiceProvider);
      final accessToken = await storage.getAccessToken();
      final refreshToken = await storage.getRefreshToken();

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
        return;
      }

      final repository = ref.read(authRepositoryProvider);
      final newTokens = await repository.refreshToken(refreshToken);
      await storage.saveTokens(
        accessToken: newTokens.accessToken,
        refreshToken: newTokens.refreshToken,
      );
      await _saveUserIdFromToken(newTokens);
      state = AuthState.authenticated(newTokens);
    } on UnauthorizedException {
      final storage = ref.read(secureStorageServiceProvider);
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    } on NetworkException {
      state = const AuthState.unauthenticated();
    } catch (_) {
      final storage = ref.read(secureStorageServiceProvider);
      await storage.clearAll();
      state = const AuthState.unauthenticated();
    }
  }

  Future<void> login(String email, String password) async {
    state = const AuthState.loading();
    try {
      final repository = ref.read(authRepositoryProvider);
      final tokens = await repository.login(email, password);
      final storage = ref.read(secureStorageServiceProvider);
      await storage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      );
      await _saveUserIdFromToken(tokens);
      state = AuthState.authenticated(tokens);
    } on ApiException catch (e) {
      state = AuthState.error(e.message);
    } catch (_) {
      state = const AuthState.error('Beklenmeyen bir hata oluştu.');
    }
  }

  Future<void> register(
    String email,
    String password, {
    bool premium = false,
  }) async {
    state = const AuthState.loading();
    try {
      final repository = ref.read(authRepositoryProvider);
      final tokens = await repository.register(email, password, premium: premium);
      final storage = ref.read(secureStorageServiceProvider);
      await storage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      );
      await _saveUserIdFromToken(tokens);
      state = AuthState.authenticated(tokens);
    } on ApiException catch (e) {
      state = AuthState.error(e.message);
    } catch (_) {
      state = const AuthState.error('Beklenmeyen bir hata oluştu.');
    }
  }

  Future<void> logout() async {
    final storage = ref.read(secureStorageServiceProvider);
    await storage.clearAll();
    state = const AuthState.unauthenticated();
  }

  Future<void> _saveUserIdFromToken(AuthTokens tokens) async {
    final userId = tokens.userId;
    if (userId != null) {
      await ref.read(secureStorageServiceProvider).saveUserId(userId);
    }
  }
}
