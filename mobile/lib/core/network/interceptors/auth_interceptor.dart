import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../features/auth/domain/entities/auth_tokens.dart';
import '../../../features/auth/presentation/providers/auth_provider.dart';
import '../../../features/auth/presentation/providers/auth_state.dart';
import '../../constants/api_constants.dart';
import '../../storage/secure_storage_service.dart';
import '../dio_client.dart';

class AuthInterceptor extends QueuedInterceptorsWrapper {
  AuthInterceptor(this._ref);

  final Ref _ref;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    // Prefer the in-memory token from auth state — avoids Keychain during the
    // window after clearKeychain (E2E tests) where securityd is restarting and
    // Keychain reads may hang or throw errSecInteractionNotAllowed.
    final authState = _ref.read(authNotifierProvider);
    String? token;
    if (authState is AuthStateAuthenticated) {
      token = authState.tokens.accessToken;
    } else {
      final storage = _ref.read(secureStorageServiceProvider);
      token = await _safeKeychainRead(storage.getAccessToken());
    }
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  static Future<String?> _safeKeychainRead(Future<String?> read) async {
    try {
      return await read.timeout(
        const Duration(seconds: 3),
        onTimeout: () => null,
      );
    } catch (_) {
      return null;
    }
  }

  bool _isTokenError(DioException err) {
    final status = err.response?.statusCode;
    if (status == 401) return true;
    if (status == 403) {
      final error = err.response?.data;
      final message = error is Map ? (error['error'] as String?) : null;
      return message != null && message.contains('JWT');
    }
    return false;
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (!_isTokenError(err)) {
      handler.next(err);
      return;
    }
    if (err.requestOptions.extra['_retry'] == true) {
      // Retry already attempted — don't loop. Don't logout either:
      // the refresh succeeded so the session is still valid; this 401 is
      // a transient server-side issue (clock skew / token cache lag).
      handler.next(err);
      return;
    }

    // Prefer in-memory refresh token (always available, even when Keychain
    // writes fail on simulator with -34018: errSecMissingEntitlement).
    final authState = _ref.read(authNotifierProvider);
    final storage = _ref.read(secureStorageServiceProvider);
    String? refreshToken;
    if (authState is AuthStateAuthenticated) {
      refreshToken = authState.tokens.refreshToken;
    } else {
      refreshToken = await _safeKeychainRead(storage.getRefreshToken());
    }

    if (refreshToken == null) {
      await _logout();
      handler.next(err);
      return;
    }

    // ── Concurrent-refresh guard ─────────────────────────────────────────────
    // QueuedInterceptorsWrapper serializes onError callbacks. By the time a
    // queued 401 runs here, a previous call may have already refreshed the
    // token. Detect this: if the in-memory token differs from the one that
    // failed, we already have fresh tokens — just retry without a 2nd refresh.
    final currentToken = authState is AuthStateAuthenticated
        ? authState.tokens.accessToken
        : null;
    final failedToken =
        (err.requestOptions.headers['Authorization'] as String?)
            ?.replaceFirst('Bearer ', '');
    if (currentToken != null &&
        failedToken != null &&
        currentToken != failedToken) {
      final retryOptions = err.requestOptions;
      retryOptions.headers['Authorization'] = 'Bearer $currentToken';
      retryOptions.extra['_retry'] = true;
      try {
        final retryResponse =
            await _ref.read(refreshDioProvider).fetch(retryOptions);
        handler.resolve(retryResponse);
      } catch (_) {
        // Fresh token still rejected — transient server issue; pass through
        // without logging out (session is still valid).
        handler.next(err);
      }
      return;
    }

    // Track whether the refresh POST itself succeeded so we only logout when
    // the refresh token is truly invalid — not when the retry of the original
    // request fails transiently (e.g. server-side clock skew / cache lag).
    var refreshSucceeded = false;
    try {
      final refreshDio = _ref.read(refreshDioProvider);
      final response = await refreshDio.post(
        ApiConstants.refreshToken,
        options: Options(headers: {'Authorization': 'Bearer $refreshToken'}),
      );

      final data = response.data as Map<String, dynamic>;
      final newAccessToken = data['token'] as String;
      final newRefreshToken = (data['refreshToken'] as String?) ?? refreshToken;

      try {
        await storage.saveTokens(
          accessToken: newAccessToken,
          refreshToken: newRefreshToken,
        );
      } catch (_) {
        // Keychain write may fail on simulator (-34018: errSecMissingEntitlement);
        // in-memory state below keeps the session alive.
      }
      // Update in-memory auth state synchronously so subsequent requests use
      // the new access token immediately (Keychain write above may have failed
      // silently on simulator with -34018: errSecMissingEntitlement).
      _ref.read(authNotifierProvider.notifier).updateTokensFromInterceptor(
        AuthTokens(accessToken: newAccessToken, refreshToken: newRefreshToken),
      );
      refreshSucceeded = true;

      final retryOptions = err.requestOptions;
      retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';
      retryOptions.extra['_retry'] = true;
      final retryResponse = await refreshDio.fetch(retryOptions);
      handler.resolve(retryResponse);
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      // Only logout when the refresh token call itself failed — if the refresh
      // succeeded but the retry of the original request got a 401 (transient
      // server-side issue), the session is still valid so don't logout.
      if (!refreshSucceeded && (status == 401 || status == 403 || status == 400)) {
        await _logout();
      }
      handler.next(err);
    } catch (_) {
      handler.next(err);
    }
  }

  Future<void> _logout() async {
    final storage = _ref.read(secureStorageServiceProvider);
    await storage.clearAll();
    _ref.read(authNotifierProvider.notifier).logout();
  }
}
