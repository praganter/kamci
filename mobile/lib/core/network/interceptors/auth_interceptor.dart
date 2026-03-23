import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../features/auth/presentation/providers/auth_provider.dart';
import '../../constants/api_constants.dart';
import '../../storage/secure_storage_service.dart';
import '../dio_client.dart';

class AuthInterceptor extends QueuedInterceptorsWrapper {
  AuthInterceptor(this._ref);

  final Ref _ref;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final storage = _ref.read(secureStorageServiceProvider);
    final token = await storage.getAccessToken();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
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
      await _logout();
      handler.next(err);
      return;
    }

    final storage = _ref.read(secureStorageServiceProvider);
    final refreshToken = await storage.getRefreshToken();

    if (refreshToken == null) {
      await _logout();
      handler.next(err);
      return;
    }

    try {
      final refreshDio = _ref.read(refreshDioProvider);
      final response = await refreshDio.post(
        ApiConstants.refreshToken,
        options: Options(headers: {'Authorization': 'Bearer $refreshToken'}),
      );

      final data = response.data as Map<String, dynamic>;
      final newAccessToken = data['token'] as String;
      final newRefreshToken = (data['refreshToken'] as String?) ?? refreshToken;

      await storage.saveTokens(accessToken: newAccessToken, refreshToken: newRefreshToken);

      final retryOptions = err.requestOptions;
      retryOptions.headers['Authorization'] = 'Bearer $newAccessToken';
      retryOptions.extra['_retry'] = true;
      final retryResponse = await refreshDio.fetch(retryOptions);
      handler.resolve(retryResponse);
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      if (status == 401 || status == 403 || status == 400) {
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
