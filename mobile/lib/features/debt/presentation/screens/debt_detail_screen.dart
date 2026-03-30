import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/router/route_names.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../domain/entities/payment_embedded.dart';
import '../providers/debt_providers.dart';

class DebtDetailScreen extends ConsumerWidget {
  const DebtDetailScreen({super.key, required this.id});
  final String id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final debtAsync = ref.watch(debtDetailProvider(id));
    final currentUserId = ref.watch(currentUserIdProvider).valueOrNull;

    return debtAsync.when(
      loading: () => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(),
        body: AppErrorWidget(
          message: e.toString(),
          onRetry: () => ref.invalidate(debtDetailProvider(id)),
        ),
      ),
      data: (debt) {
        final isOwner = currentUserId == debt.ownerId;
        final currenciesAsync = ref.watch(currencyListProvider);
        final currencySymbol = currenciesAsync.maybeWhen(
          data: (list) =>
              list.firstWhere((c) => c.code == (debt.currency ?? 'TRY'),
                  orElse: () => list.firstWhere((c) => c.code == 'TRY',
                      orElse: () => list.first)).symbol,
          orElse: () => '₺',
        );
        final decimalDigits = debt.amount % 1 == 0 ? 0 : 2;
        final currencyFormat = NumberFormat.currency(
          locale: 'tr_TR',
          symbol: currencySymbol,
          decimalDigits: decimalDigits,
        );

        return Scaffold(
          appBar: AppBar(
            title: Text(debt.name),
            actions: [
              if (isOwner) ...[
                IconButton(
                  icon: const Icon(Icons.edit_outlined),
                  onPressed: () => context.pushNamed(
                    RouteNames.debtEdit,
                    pathParameters: {'id': id},
                  ),
                ),
                IconButton(
                  icon: Icon(Icons.delete_outline,
                      color: Theme.of(context).colorScheme.error),
                  onPressed: () async {
                    final confirmed = await showConfirmDialog(
                      context,
                      title: l10n.deleteDebt,
                      content: l10n.deleteDebtConfirm(debt.name),
                    );
                    if (confirmed && context.mounted) {
                      await ref
                          .read(debtListProvider.notifier)
                          .deleteDebt(id);
                      if (context.mounted) context.pop();
                    }
                  },
                ),
              ],
            ],
          ),
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _InfoCard(
                  children: [
                    _InfoRow(l10n.amount, currencyFormat.format(debt.amount)),
                    _InfoRow(l10n.status, debt.status.l10nLabel(l10n)),
                    _InfoRow(l10n.paymentPeriod,
                        debt.paymentPeriodType.l10nLabel(l10n)),
                    _InfoRow(l10n.startDate,
                        DateFormatter.formatDate(debt.firstPaymentDate)),
                    _InfoRow(
                      l10n.installmentCount,
                      debt.installmentCount != null
                          ? l10n.installmentProgress(
                              debt.paidInstallmentCount ?? 0,
                              debt.installmentCount!)
                          : l10n.openEnded,
                    ),
                    _InfoRow(l10n.autoPayment,
                        debt.autoPayment ? '✓' : '—'),
                    if (debt.description != null)
                      _InfoRow(l10n.description, debt.description!),
                    if (debt.nextReminderDate != null)
                      _InfoRow(l10n.nextReminder,
                          DateFormatter.formatDate(debt.nextReminderDate!)),
                  ],
                ),
                const SizedBox(height: 24),
                Text(
                  l10n.paymentHistory(debt.payments.length),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 12),
                if (debt.payments.isEmpty)
                  Center(child: Text(l10n.noPaymentsYet))
                else
                  ...debt.payments
                      .map((p) => _PaymentTile(payment: p, l10n: l10n)),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.children});
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(children: children),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context)
                        .colorScheme
                        .onSurface
                        .withValues(alpha: 0.6),
                  ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w500,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PaymentTile extends StatelessWidget {
  const _PaymentTile({required this.payment, required this.l10n});
  final PaymentEmbedded payment;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return ListTile(
      leading: Container(
        width: 32,
        height: 32,
        decoration: BoxDecoration(
          color: payment.paid
              ? AppColors.statusActive.withValues(alpha: 0.12)
              : cs.surfaceContainerHigh,
          shape: BoxShape.circle,
        ),
        child: Icon(
          payment.paid ? Icons.check_rounded : Icons.schedule_rounded,
          size: 16,
          color: payment.paid ? AppColors.statusActive : cs.onSurfaceVariant,
        ),
      ),
      title: Text(payment.paid ? l10n.paid : l10n.pendingPayment),
      subtitle: payment.paidTime != null
          ? Text(DateFormatter.formatDateTime(payment.paidTime!))
          : null,
    );
  }
}
