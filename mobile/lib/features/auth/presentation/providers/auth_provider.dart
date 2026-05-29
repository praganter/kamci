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
        final email = await storage.getEmail();
        ref.read(notificationServiceProvider).registerToken(email: email);
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
      final email = await storage.getEmail();
      ref.read(notificationServiceProvider).registerToken(email: email);
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
      await storage.saveEmail(email);
      await _saveUserIdFromToken(tokens);
      await ref.read(notificationServiceProvider).registerToken(email: email);
      state = AuthState.authenticated(tokens);
    } on UnauthorizedException {
      state = const AuthState.error('E-posta veya şifre hatalı.');
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
      final tokens = await repository.register(
        email,
        password,
        premium: premium,
      );
      final storage = ref.read(secureStorageServiceProvider);
      await storage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      );
      await storage.saveEmail(email);
      await _saveUserIdFromToken(tokens);
      await ref.read(notificationServiceProvider).registerToken(email: email);
      state = AuthState.authenticated(tokens);
    } on ApiException catch (e) {
      state = AuthState.error(e.message);
    } catch (_) {
      state = const AuthState.error('Beklenmeyen bir hata oluştu.');
    }
  }

  Future<void> logout() async {
    await ref.read(notificationServiceProvider).unregisterToken();
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

@riverpod
Future<UserDetail> userDetail(Ref ref) =>
    ref.watch(authRepositoryProvider).userDetail();
