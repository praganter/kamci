import '../network/api_exception.dart';
import 'l10n.dart';

extension ApiExceptionL10n on ApiException {
  String toLocalizedMessage(AppLocalizations l10n) => switch (this) {
        BusinessException(:final code) =>
          _businessCodeMessage(code, l10n) ?? message,
        PremiumLimitException() => message,
        PermissionDeniedException() => message,
        NetworkException() => l10n.errorNetwork,
        UnauthorizedException() => l10n.errorSessionExpired,
        NotFoundException() => l10n.errorNotFound,
        ServerException() => l10n.errorServer,
        RateLimitException() => l10n.errorRateLimit,
        UnknownException() => l10n.errorUnexpected,
      };
}

/// Localized copy for the recognized [BusinessException.code] values.
/// Returns `null` for an unknown/absent code so the caller falls back to the
/// backend-supplied message.
String? _businessCodeMessage(String? code, AppLocalizations l10n) =>
    switch (code) {
      'error.debt.already_paid' => l10n.errorDebtAlreadyPaid,
      'error.debt.overpayment' => l10n.errorDebtOverpayment,
      'error.category.name_conflict' => l10n.errorCategoryNameConflict,
      'error.category.cannot_add_self' => l10n.errorCategoryCannotAddSelf,
      'error.category.member_not_found' => l10n.errorCategoryMemberNotFound,
      _ => null,
    };

extension ErrorL10n on Object {
  String toLocalizedMessage(AppLocalizations l10n) {
    if (this is ApiException) return (this as ApiException).toLocalizedMessage(l10n);
    return toString();
  }
}
