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

String? _businessCodeMessage(String? code, AppLocalizations l10n) =>
    switch (code) {
      'error.debt.already_completed' => l10n.errorDebtAlreadyCompleted,
      'error.debt.overpayment' => l10n.errorDebtOverpayment,
      'error.category.name_conflict' => l10n.errorCategoryNameConflict,
      'error.category.cannot_add_self' => l10n.errorCategoryCannotAddSelf,
      'error.category.member_not_found' => l10n.errorCategoryMemberNotFound,
      'error.category.not_found' => l10n.errorCategoryNotFound,
      'error.category.member_user_not_found' =>
        l10n.errorCategoryMemberUserNotFound,
      'error.category.member_already_exists' =>
        l10n.errorCategoryMemberAlreadyExists,
      'error.category.migrate_same_category' =>
        l10n.errorCategoryMigrateSameCategory,
      'error.debt.not_found' => l10n.errorDebtNotFound,
      'error.debt.already_paid_this_period' =>
        l10n.errorDebtAlreadyPaidThisPeriod,
      'error.debt.auto_payment_enabled' => l10n.errorDebtAutoPaymentEnabled,
      'error.debt.concurrent_request' => l10n.errorDebtConcurrentRequest,
      'error.debt.payment_before_start' => l10n.errorDebtPaymentBeforeStart,
      'error.debt.period_already_recorded' =>
        l10n.errorDebtPeriodAlreadyRecorded,
      'error.debt.installment_count_exceeded' =>
        l10n.errorDebtInstallmentCountExceeded,
      'error.debt.missing_period_config' =>
        l10n.errorDebtMissingPeriodConfig,
      'error.income.not_found' => l10n.errorIncomeNotFound,
      'error.reminder.date_or_time_missing' =>
        l10n.errorReminderDateOrTimeMissing,
      'error.subscription.verification_failed' ||
      'error.subscription.product_not_allowed' ||
      'error.subscription.package_not_allowed' =>
        l10n.errorSubscriptionVerificationFailed,
      'error.subscription.not_found' => l10n.errorSubscriptionNotFound,
      'error.subscription.already_used' => l10n.errorSubscriptionAlreadyUsed,
      'error.auth.email_in_use' => l10n.errorAuthEmailInUse,
      'error.auth.too_many_login_attempts' =>
        l10n.errorAuthTooManyLoginAttempts,
      'error.auth.not_valid' => l10n.errorAuthNotValid,
      'error.auth.token_revoked' => l10n.errorAuthTokenRevoked,
      'error.auth.token_invalid' => l10n.errorAuthTokenInvalid,
      'error.auth.current_password_invalid' =>
        l10n.errorAuthCurrentPasswordInvalid,
      'error.auth.token_already_used' => l10n.errorAuthTokenAlreadyUsed,
      'error.auth.too_many_password_reset_attempts' =>
        l10n.errorAuthTooManyPasswordResetAttempts,
      'error.household.invitee_not_found' =>
        l10n.errorHouseholdInviteeNotFound,
      'error.household.cannot_invite_self' =>
        l10n.errorHouseholdCannotInviteSelf,
      'error.household.already_linked' => l10n.errorHouseholdAlreadyLinked,
      'error.household.invitee_already_linked' =>
        l10n.errorHouseholdInviteeAlreadyLinked,
      'error.household.not_found' => l10n.errorHouseholdNotFound,
      'error.household.not_pending' => l10n.errorHouseholdNotPending,
      _ => null,
    };

extension ErrorL10n on Object {
  String toLocalizedMessage(AppLocalizations l10n) {
    if (this is ApiException) return (this as ApiException).toLocalizedMessage(l10n);
    return toString();
  }
}
