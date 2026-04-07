import 'package:dio/dio.dart';

sealed class ApiException implements Exception {
  const ApiException(this.message);
  final String message;

  @override
  String toString() => message;

  factory ApiException.fromDioException(DioException e) {
    final statusCode = e.response?.statusCode;
    final data = e.response?.data;

    // Handle business errors (status 400, 403, 409 etc.)
    if (data is Map<String, dynamic>) {
      final message = data['message'] as String?;

      if (statusCode == 400) {
        return BusinessException(message ?? 'Geçersiz istek.');
      }
      if (statusCode == 401) {
        return const UnauthorizedException();
      }
      if (statusCode == 403) {
        return PremiumLimitException(
          message ?? 'Bu özellik premium üyeler içindir.',
        );
      }
      if (statusCode == 409) {
        return BusinessException(
          message ?? 'Bu işlem şu an gerçekleştirilemiyor.',
        );
      }
      if (statusCode == 422) {
        final errors = data['errors'] as List?;
        final errorMsg = errors != null && errors.isNotEmpty
            ? (errors.first['message'] as String?)
            : message;
        return BusinessException(errorMsg ?? 'Doğrulama hatası.');
      }
    }

    if (statusCode == 404) return const NotFoundException();

    if (statusCode != null && statusCode >= 500) {
      final msg =
          (data is Map ? data['message'] as String? : null) ??
          'Sunucu hatası oluştu.';
      return ServerException(msg);
    }

    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.connectionError) {
      return const NetworkException();
    }

    return const UnknownException();
  }
}

class NetworkException extends ApiException {
  const NetworkException([
    super.message = 'İnternet bağlantısı yok veya sunucuya ulaşılamıyor.',
  ]);
}

class UnauthorizedException extends ApiException {
  const UnauthorizedException([
    super.message = 'Oturum süresi doldu. Lütfen tekrar giriş yapın.',
  ]);
}

class NotFoundException extends ApiException {
  const NotFoundException([super.message = 'İstenen kaynak bulunamadı.']);
}

class ServerException extends ApiException {
  const ServerException([
    super.message = 'Sunucu hatası oluştu. Lütfen tekrar deneyin.',
  ]);
}

class UnknownException extends ApiException {
  const UnknownException([super.message = 'Beklenmeyen bir hata oluştu.']);
}

class PremiumLimitException extends ApiException {
  const PremiumLimitException([
    super.message = 'Bu özellik premium üyeler içindir.',
  ]);
}

class BusinessException extends ApiException {
  const BusinessException(super.message);
}
