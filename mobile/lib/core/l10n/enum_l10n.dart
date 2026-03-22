import '../../features/debt/domain/entities/debt.dart';
import '../../gen/l10n/app_localizations.dart';

extension PaymentPeriodTypeL10n on PaymentPeriodType {
  String l10nLabel(AppLocalizations l10n) => switch (this) {
        PaymentPeriodType.daily => l10n.periodDaily,
        PaymentPeriodType.weekly => l10n.periodWeekly,
        PaymentPeriodType.monthly => l10n.periodMonthly,
        PaymentPeriodType.quarterly => l10n.periodQuarterly,
        PaymentPeriodType.yearly => l10n.periodYearly,
      };
}

extension DebtStatusL10n on DebtStatus {
  String l10nLabel(AppLocalizations l10n) => switch (this) {
        DebtStatus.active => l10n.statusActive,
        DebtStatus.passive => l10n.statusInactive,
      };
}
