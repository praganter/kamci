import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/providers/app_settings_provider.dart';
import '../../../../core/router/route_names.dart';
import '../../../../core/theme/app_colors.dart';
import '../../domain/entities/debt.dart';
import '../providers/debt_providers.dart';

class CompactDebtTile extends ConsumerWidget {
  const CompactDebtTile({super.key, required this.debt});

  final Debt debt;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final currenciesAsync = ref.watch(currencyListProvider);
    final currencySymbol = currenciesAsync.maybeWhen(
      data: (list) {
        final code = debt.currency ?? 'TRY';
        return list
            .firstWhere(
              (c) => c.code == code,
              orElse: () => list.firstWhere(
                (c) => c.code == 'TRY',
                orElse: () => list.first,
              ),
            )
            .symbol;
      },
      orElse: () => '₺',
    );
    final decimalDigits = debt.amount % 1 == 0 ? 0 : 2;
    final currencyFormat = NumberFormat.currency(
      locale: 'tr_TR',
      symbol: currencySymbol,
      decimalDigits: decimalDigits,
    );

    final baseCurrency = ref.watch(baseCurrencyProvider);
    final ratesAsync = ref.watch(exchangeRatesProvider);
    final debtCurrency = debt.currency ?? 'TRY';
    String? convertedLabel;
    if (debtCurrency != baseCurrency) {
      final rate = ratesAsync.maybeWhen(
        data: (rates) => rates[debtCurrency],
        orElse: () => null,
      );
      if (rate != null && rate > 0) {
        final converted = debt.amount / rate;
        final baseSymbol = currenciesAsync.maybeWhen(
          data: (list) => list
              .firstWhere(
                (c) => c.code == baseCurrency,
                orElse: () => list.first,
              )
              .symbol,
          orElse: () => baseCurrency,
        );
        final baseFmt = NumberFormat.currency(
          locale: 'tr_TR',
          symbol: baseSymbol,
          decimalDigits: 0,
        );
        convertedLabel = '≈ ${baseFmt.format(converted)}';
      }
    }

    final cs = Theme.of(context).colorScheme;

    final statusColor = switch (debt.status) {
      DebtStatus.active => AppColors.statusActive,
      DebtStatus.completed => AppColors.statusInactive,
    };
    final statusLabel = switch (debt.status) {
      DebtStatus.active => l10n.statusActive,
      DebtStatus.completed => l10n.statusInactive,
    };
    final statusIcon = switch (debt.status) {
      DebtStatus.active => Icons.radio_button_checked_rounded,
      DebtStatus.completed => Icons.pause_circle_outline_rounded,
    };

    final paymentDotColor = switch (debt.status) {
      DebtStatus.completed => AppColors.statusInactive,
      DebtStatus.active => switch (debt.paymentStatus) {
        PaymentStatus.paid => AppColors.statusActive,
        _ => const Color(0xFFEF4444),
      },
    };

    return InkWell(
      onTap: () => context.pushNamed(
        RouteNames.debtDetail,
        pathParameters: {'id': debt.id},
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
        child: Row(
          children: [
            Container(
              width: 8,
              height: 8,
              margin: const EdgeInsets.only(right: 10, top: 1),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: paymentDotColor,
                boxShadow: debt.status == DebtStatus.active
                    ? [
                        BoxShadow(
                          color: paymentDotColor.withValues(alpha: 0.5),
                          blurRadius: 4,
                          spreadRadius: 1,
                        ),
                      ]
                    : null,
              ),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    debt.name,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Wrap(
                    spacing: 6,
                    runSpacing: 4,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 7,
                          vertical: 3,
                        ),
                        decoration: BoxDecoration(
                          color: statusColor.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: statusColor.withValues(alpha: 0.3),
                          ),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(statusIcon, size: 10, color: statusColor),
                            const SizedBox(width: 3),
                            Text(
                              statusLabel,
                              style: Theme.of(context).textTheme.labelSmall
                                  ?.copyWith(
                                    color: statusColor,
                                    fontWeight: FontWeight.w600,
                                  ),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 7,
                          vertical: 3,
                        ),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: cs.outlineVariant),
                        ),
                        child: Text(
                          debt.paymentPeriodType.l10nLabel(l10n),
                          style: Theme.of(context).textTheme.labelSmall
                              ?.copyWith(
                                color: cs.onSurfaceVariant,
                                fontWeight: FontWeight.w500,
                              ),
                        ),
                      ),
                      if (debt.installmentCount != null &&
                          debt.installmentCount! > 0)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 7,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: cs.outlineVariant),
                          ),
                          child: Text(
                            l10n.installmentProgress(
                              debt.paidInstallmentCount ?? 0,
                              debt.installmentCount!,
                            ),
                            style: Theme.of(context).textTheme.labelSmall
                                ?.copyWith(
                                  color: cs.onSurfaceVariant,
                                  fontWeight: FontWeight.w500,
                                ),
                          ),
                        )
                      else if (debt.paidInstallmentCount != null &&
                          debt.paidInstallmentCount! > 0)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 7,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: cs.outlineVariant),
                          ),
                          child: Text(
                            l10n.openEndedProgress(debt.paidInstallmentCount!),
                            style: Theme.of(context).textTheme.labelSmall
                                ?.copyWith(
                                  color: cs.onSurfaceVariant,
                                  fontWeight: FontWeight.w500,
                                ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  currencyFormat.format(debt.amount),
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: cs.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                if (convertedLabel != null)
                  Text(
                    convertedLabel,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: cs.onSurfaceVariant,
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
