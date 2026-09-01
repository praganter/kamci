import '../network/api_exception.dart';
import 'l10n.dart';

extension ApiExceptionL10n on ApiException {
  String toLocalizedMessage(AppLocalizations l10n) => switch (this) {
        BusinessException() => message,
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

extension ErrorL10n on Object {
  String toLocalizedMessage(AppLocalizations l10n) {
    if (this is ApiException) return (this as ApiException).toLocalizedMessage(l10n);
    return toString();
  }
}
