import 'package:dio/dio.dart';

sealed class ApiException implements Exception {
  const ApiException(this.message);
  final String message;

  @override
  String toString() => message;

  /// `ErrorResponse.code` values that mean "this action needs premium /
  /// hit a free-tier limit" — regardless of whether they arrive as 400
  /// (limit exceeded) or 403 (premium required for the feature).
  static const _premiumCodes = {
    'error.debt.limit_exceeded',
    'error.category.limit_exceeded',
    'error.category.premium_required',
    'error.debt.premium_required',
  };

  /// `code` values that mean "you don't own/aren't allowed this row" — a
  /// 403 that must NOT be routed to the premium-upsell flow.
  static const _permissionDeniedCodes = {
    'error.debt.permission_denied',
    'error.category.permission_denied',
    'error.income.permission_denied',
  };

  /// `code` values that just need a specific localized message — no special
  /// UI (no upsell sheet, no session reset). They can arrive as 400, 404 or
  /// 409; the status alone can't tell them apart from generic validation /
  /// not-found. Carried on [BusinessException.code] and resolved to copy in
  /// `exception_l10n.dart` (falls back to the backend `message`).
  static const _businessCodes = {
    'error.debt.already_completed', // 409 — /debt/pay on a COMPLETED (paid/skipped) debt
    'error.debt.overpayment', // 400 — pay/bulkPay amount exceeds the debt
    'error.category.name_conflict', // 409 — same owner, case-insensitive name
    'error.category.cannot_add_self', // 409 — owner adds themselves as member
    'error.category.member_not_found', // 404 — removing a non-member
    'error.category.not_found',
    'error.category.member_user_not_found',
    'error.category.member_already_exists',
    'error.category.migrate_same_category',
    'error.debt.not_found',
    'error.debt.already_paid_this_period',
    'error.debt.auto_payment_enabled',
    'error.debt.concurrent_request',
    'error.debt.payment_before_start',
    'error.debt.period_already_recorded',
    'error.debt.installment_count_exceeded',
    'error.debt.missing_period_config',
    'error.income.not_found',
    'error.reminder.date_or_time_missing',
    'error.subscription.verification_failed',
    'error.subscription.not_found',
    'error.auth.email_in_use',
    'error.auth.too_many_login_attempts',
    'error.auth.not_valid',
    'error.auth.token_revoked',
    'error.auth.token_invalid',
    'error.auth.current_password_invalid',
    'error.auth.token_already_used',
    'error.auth.too_many_password_reset_attempts',
  };

  factory ApiException.fromDioException(DioException e) {
    final statusCode = e.response?.statusCode;
    final data = e.response?.data;

    if (data is Map<String, dynamic>) {
      final message = data['message'] as String?;
      final code = data['code'] as String?;

      // Prefer the stable `code` over the HTTP status — the backend reuses
      // 400/403 for several distinct situations that a status code alone
      // can't disambiguate (limit vs. validation, premium vs. permission).
      if (code != null) {
        if (_premiumCodes.contains(code)) {
          return PremiumLimitException(message ?? const PremiumLimitException().message);
        }
        if (_permissionDeniedCodes.contains(code)) {
          return PermissionDeniedException(message ?? const PermissionDeniedException().message);
        }
        if (code == 'error.user.not_found_in_context') {
          return UnauthorizedException(message ?? const UnauthorizedException().message);
        }
        if (_businessCodes.contains(code)) {
          return BusinessException(
            message ?? 'This operation cannot be completed right now.',
            code: code,
          );
        }
      }

      if (statusCode == 400) {
        return BusinessException(message ?? 'Invalid request.');
      }
      if (statusCode == 401) {
        return UnauthorizedException(message ?? const UnauthorizedException().message);
      }
      if (statusCode == 403) {
        // No recognized `code` — fall back to the pre-C2 behavior (treat as
        // a premium/limit wall) rather than guessing permission-denied.
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

/// The request was understood and authenticated, but the caller doesn't own
/// or have rights to the target row (e.g. editing a category they only have
/// READ access to). Distinct from [PremiumLimitException]: this is NOT a
/// premium-upsell situation — no sheet, just an error message.
class PermissionDeniedException extends ApiException {
  const PermissionDeniedException([
    super.message = "You don't have permission to perform this action.",
  ]);
}

class BusinessException extends ApiException {
  const BusinessException(super.message, {this.code});

  /// Optional backend `ErrorResponse.code`. When set and recognized,
  /// `exception_l10n.dart` renders localized copy instead of [message].
  final String? code;
}

class RateLimitException extends ApiException {
  const RateLimitException([
    super.message = 'Too many requests. Please wait a moment.',
  ]);
}
