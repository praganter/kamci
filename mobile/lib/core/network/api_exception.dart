import 'package:dio/dio.dart';

sealed class ApiException implements Exception {
  const ApiException(this.message);
  final String message;

  @override
  String toString() => message;

  factory ApiException.fromDioException(DioException e) {
    final statusCode = e.response?.statusCode;
    final data = e.response?.data;

    if (data is Map<String, dynamic>) {
      final message = data['message'] as String?;

      if (statusCode == 400) {
        return BusinessException(message ?? 'Invalid request.');
      }
      if (statusCode == 401) {
        return UnauthorizedException(message ?? const UnauthorizedException().message);
      }
      if (statusCode == 403) {
        return PremiumLimitException(message ?? const PremiumLimitException().message);
      }
      if (statusCode == 409) {
        return BusinessException(message ?? 'This operation cannot be completed right now.');
      }
      if (statusCode == 422) {
        final errors = data['errors'] as List?;
        final errorMsg = errors != null && errors.isNotEmpty
            ? (errors.first['message'] as String?)
            : message;
        return BusinessException(errorMsg ?? 'Validation error.');
      }
    }

    if (statusCode == 404) return const NotFoundException();
    if (statusCode == 429) return const RateLimitException();

    if (statusCode != null && statusCode >= 500) {
      final msg =
          (data is Map ? data['message'] as String? : null) ??
          const ServerException().message;
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
    super.message = 'No internet connection or server unreachable.',
  ]);
}

class UnauthorizedException extends ApiException {
  const UnauthorizedException([
    super.message = 'Session expired. Please sign in again.',
  ]);
}

class NotFoundException extends ApiException {
  const NotFoundException([super.message = 'The requested resource was not found.']);
}

class ServerException extends ApiException {
  const ServerException([
    super.message = 'A server error occurred. Please try again.',
  ]);
}

class UnknownException extends ApiException {
  const UnknownException([super.message = 'An unexpected error occurred.']);
}

class PremiumLimitException extends ApiException {
  const PremiumLimitException([
    super.message = 'This feature is available to premium members only.',
  ]);
}

class BusinessException extends ApiException {
  const BusinessException(super.message);
}

class RateLimitException extends ApiException {
  const RateLimitException([
    super.message = 'Too many requests. Please wait a moment.',
  ]);
}
