import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../features/auth/domain/entities/auth_tokens.dart';
import '../../../features/auth/presentation/providers/auth_provider.dart';
import '../../../features/auth/presentation/providers/auth_state.dart';
import '../../constants/api_constants.dart';
import '../../storage/secure_storage_service.dart';
import '../dio_client.dart';

enum _TokenErrorAction {
  none,

  refresh,

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

  static const _credentialCheckPaths = [
    '/auth/change-password',
    '/auth/reset-password',
  ];

  static const _tokenExpiredCodes = {
    'error.auth.token_expired',
    'error.auth.header_missing',
    'error.auth.header_invalid',
  };

  static const _tokenInvalidCode = 'error.auth.token_invalid';

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
      handler.next(err);
      return;
    }

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
        handler.next(err);
      }
      return;
    }

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
      }
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
