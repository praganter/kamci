import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../features/auth/domain/entities/auth_tokens.dart';
import '../../../features/auth/presentation/providers/auth_provider.dart';
import '../../../features/auth/presentation/providers/auth_state.dart';
import '../../constants/api_constants.dart';
import '../../storage/secure_storage_service.dart';
import '../dio_client.dart';

enum _TokenErrorAction {
  /// Not a token error — pass through untouched.
  none,

  /// Attempt the normal refresh-then-retry flow.
  refresh,

  /// Skip refresh entirely and end the session immediately.
  forceLogout,
}

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

  /// Endpoints where a 401/403 means "wrong credentials supplied in the body"
  /// (current password, reset token) — NOT an expired session. Refreshing here
  /// is wrong: it wastes a refresh round-trip, retries the bad credentials, and
  /// can mask the real error. Let these pass straight through to the datasource
  /// so `ApiException.fromDioException` maps them to a UnauthorizedException /
  /// BusinessException the UI can show.
  static const _credentialCheckPaths = [
    '/auth/change-password',
    '/auth/reset-password',
  ];

  /// F5 (2026-09-01): gateway is rolling out stable error codes for
  /// auth/rate-limit failures (`TOKEN_EXPIRED`, `TOKEN_INVALID`,
  /// `AUTH_HEADER_MISSING`, `AUTH_HEADER_INVALID`, `RATE_LIMITED`) to replace
  /// the old free-text `"JWT is invalid or expired"` / `JWT_EXPIRED` /
  /// `INVALID_JWT` strings. Gateway and mobile deploy independently, so this
  /// interceptor recognizes BOTH: the new codes take priority, the old
  /// substring match stays as a fallback for as long as any backend instance
  /// still speaks the old shape. Drop the fallback once the rollout is
  /// confirmed complete on all environments.
  static const _tokenExpiredCodes = {
    'TOKEN_EXPIRED',
    // A missing/malformed Authorization header is most likely a stale or
    // corrupted in-memory/Keychain token — the same recovery as an expired
    // token (attempt refresh; retry naturally re-attaches a fresh header)
    // fixes it, and falls through to the existing "no refresh token → logout"
    // path if there's nothing to refresh with.
    'AUTH_HEADER_MISSING',
    'AUTH_HEADER_INVALID',
  };

  /// A refresh token was sent where an access token was expected (wrong
  /// `type` claim). Retrying with a freshly refreshed access token won't fix
  /// a structurally wrong request that already proves the client's token
  /// bookkeeping is broken — end the session instead of looping on refresh.
  static const _tokenInvalidCode = 'TOKEN_INVALID';

  _TokenErrorAction _tokenErrorAction(DioException err) {
    final path = err.requestOptions.path;
    if (_credentialCheckPaths.any(path.contains)) return _TokenErrorAction.none;

    final status = err.response?.statusCode;
    final data = err.response?.data;
    final code = data is Map ? data['code'] as String? : null;

    if (code == _tokenInvalidCode) return _TokenErrorAction.forceLogout;
    if (code != null && _tokenExpiredCodes.contains(code)) {
      return _TokenErrorAction.refresh;
    }

    if (status == 401) return _TokenErrorAction.refresh;
    if (status == 403) {
      // Legacy detection (pre-`code`-rollout gateway/services): gateway 403
      // body is {"error":"JWT is invalid or expired","status":403}; services
      // send {"code":"JWT_EXPIRED"} / {"code":"INVALID_JWT"}.
      if (data is! Map) return _TokenErrorAction.none;
      final legacy = (data['code'] ?? data['error'] ?? data['message']) as String?;
      if (legacy != null && legacy.contains('JWT')) return _TokenErrorAction.refresh;
    }
    return _TokenErrorAction.none;
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final action = _tokenErrorAction(err);
    if (action == _TokenErrorAction.none) {
      handler.next(err);
      return;
    }
    if (action == _TokenErrorAction.forceLogout) {
      await _logout();
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
