import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../constants/api_constants.dart';
import '../../storage/secure_storage_service.dart';
import '../dio_client.dart';
import '../../../features/auth/presentation/providers/auth_provider.dart';

class AuthInterceptor extends QueuedInterceptorsWrapper {
  AuthInterceptor(this._ref);

  final Ref _ref;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final storage = _ref.read(secureStorageServiceProvider);
    final token = await storage.getAccessToken();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode != 401) {
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

      final newAccessToken = response.data['token'] as String;
      final newRefreshToken = response.data['refreshToken'] as String;

      await storage.saveTokens(
        accessToken: newAccessToken,
        refreshToken: newRefreshToken,
      );

      final options = err.requestOptions;
      options.headers['Authorization'] = 'Bearer $newAccessToken';
      options.extra['_retry'] = true;
      final retryResponse = await _ref.read(dioClientProvider).fetch(options);
      handler.resolve(retryResponse);
    } catch (_) {
      await _logout();
      handler.next(err);
    }
  }

  Future<void> _logout() async {
    final storage = _ref.read(secureStorageServiceProvider);
    await storage.clearAll();
    _ref.read(authNotifierProvider.notifier).logout();
  }
}
