import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/router/route_names.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/permission_utils.dart';
import '../../../../core/utils/snackbar_utils.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../../../category/domain/entities/member_embedded.dart';
import '../../domain/entities/debt.dart';
import '../../domain/entities/payment_embedded.dart';
import '../providers/debt_providers.dart';

class DebtDetailScreen extends ConsumerStatefulWidget {
  const DebtDetailScreen({super.key, required this.id});
  final String id;

  @override
  ConsumerState<DebtDetailScreen> createState() => _DebtDetailScreenState();
}

class _DebtDetailScreenState extends ConsumerState<DebtDetailScreen> {
  NumberFormat? _currencyFormat;
  String? _cachedSymbol;
  int? _cachedDecimalDigits;

  NumberFormat _getFormat(String symbol, int decimalDigits) {
    if (symbol != _cachedSymbol || decimalDigits != _cachedDecimalDigits) {
      _cachedSymbol = symbol;
      _cachedDecimalDigits = decimalDigits;
      _currencyFormat = NumberFormat.currency(
        locale: Intl.defaultLocale,
        symbol: symbol,
        decimalDigits: decimalDigits,
      );
    }
    return _currencyFormat!;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final debtAsync = ref.watch(debtDetailProvider(widget.id));
    final currentUserId = ref.watch(currentUserIdProvider).valueOrNull;

    return debtAsync.when(
      loading: () =>
          const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (e, _) => Scaffold(
        appBar: AppBar(),
        body: AppErrorWidget(
          error: e,
          onRetry: () => ref.invalidate(debtDetailProvider(widget.id)),
        ),
      ),
      data: (debt) {
        final isOwner = currentUserId == debt.ownerId;
        final accessLevels = ref
            .watch(categoryAccessLevelsProvider)
            .valueOrNull;
        final level = accessLevels?[debt.categoryId];

        final currenciesAsync = ref.watch(currencyListProvider);
        final currencySymbol = currenciesAsync.maybeWhen(
          data: (list) => list
              .firstWhere(
                (c) => c.code == (debt.currency ?? 'TRY'),
                orElse: () => list.firstWhere(
                  (c) => c.code == 'TRY',
                  orElse: () => list.first,
                ),
              )
              .symbol,
          orElse: () => '₺',
        );
        final decimalDigits = debt.amount % 1 == 0 ? 0 : 2;
        final currencyFormat = _getFormat(currencySymbol, decimalDigits);

        return Scaffold(
          appBar: AppBar(
            title: Text(debt.name),
            actions: [
              // Backend 409 döner: tamamlanmış ya da otomatik ödemeli borç.
              if (debt.status == DebtStatus.active && !debt.autoPayment)
                IconButton(
                  icon: const Icon(Icons.playlist_add_check),
                  tooltip: l10n.setPaidInstallments,
                  onPressed: () =>
                      _setPaidInstallments(isOwner: isOwner, level: level),
                ),
              IconButton(
                icon: const Icon(Icons.edit_outlined),
                tooltip: l10n.edit,
                onPressed: () async {
                  final hasEdit = PermissionUtils.check(
                    context,
                    isOwner: isOwner,
                    level: level,
                    action: (l) => l.canEdit,
                  );
                  if (!hasEdit) return;

                  await context.pushNamed(
                    RouteNames.debtEdit,
                    pathParameters: {'id': widget.id},
                  );
                  ref.invalidate(debtDetailProvider(widget.id));
                  ref.invalidate(debtGroupedProvider(true));
                  ref.invalidate(debtGroupedProvider(false));
                },
              ),
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
                    _InfoRow(
                      l10n.paymentPeriod,
                      debt.paymentPeriodType.l10nLabel(l10n),
                    ),
                    _InfoRow(
                      l10n.startDate,
                      DateFormatter.formatDate(debt.firstPaymentDate),
                    ),
                    _InfoRow(
                      l10n.installmentCount,
                      debt.installmentCount != null &&
                              debt.installmentCount! > 0
                          ? l10n.installmentProgress(
                              debt.paidInstallmentCount ?? 0,
                              debt.installmentCount!,
                            )
                          : (debt.paidInstallmentCount != null &&
                                debt.paidInstallmentCount! > 0)
                          ? l10n.openEndedProgress(debt.paidInstallmentCount!)
                          : l10n.openEnded,
                    ),
                    _InfoRow(l10n.autoPayment, debt.autoPayment ? '✓' : '—'),
                    if (debt.description != null)
                      _InfoRow(l10n.description, debt.description!),
                    if (debt.nextReminderDate != null)
                      _InfoRow(
                        l10n.nextReminder,
                        DateFormatter.formatDate(debt.nextReminderDate!),
                      ),
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
                  ...debt.payments.map(
                    (p) => _PaymentTile(payment: p, l10n: l10n),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _setPaidInstallments({
    required bool isOwner,
    required AccessLevel? level,
  }) async {
    final hasWrite = PermissionUtils.check(
      context,
      isOwner: isOwner,
      level: level,
      action: (l) => l.canWrite,
    );
    if (!hasWrite) return;

    final count = await showDialog<int>(
      context: context,
      builder: (_) => const _PaidInstallmentsDialog(),
    );
    if (count == null || !mounted) return;
    try {
      await ref
          .read(debtListProvider.notifier)
          .setPaidInstallments(widget.id, count);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(context.l10n.setPaidInstallmentsSuccess(count)),
          ),
        );
      }
    } catch (e) {
      if (mounted) showErrorSnackBar(context, e);
    }
  }
}

class _PaidInstallmentsDialog extends StatefulWidget {
  const _PaidInstallmentsDialog();

  @override
  State<_PaidInstallmentsDialog> createState() =>
      _PaidInstallmentsDialogState();
}

class _PaidInstallmentsDialogState extends State<_PaidInstallmentsDialog> {
  final _formKey = GlobalKey<FormState>();
  final _ctrl = TextEditingController();

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;
    Navigator.of(context).pop(int.parse(_ctrl.text.trim()));
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AlertDialog(
      title: Text(l10n.setPaidInstallments),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.setPaidInstallmentsDesc),
            const SizedBox(height: 16),
            TextFormField(
              controller: _ctrl,
              autofocus: true,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: InputDecoration(
                labelText: l10n.paidInstallmentsCountLabel,
              ),
              validator: (v) {
                final n = int.tryParse(v?.trim() ?? '');
                return n == null || n < 1
                    ? l10n.paidInstallmentsCountInvalid
                    : null;
              },
              onFieldSubmitted: (_) => _submit(),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(l10n.cancel),
        ),
        FilledButton(onPressed: _submit, child: Text(l10n.save)),
      ],
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
                color: Theme.of(
                  context,
                ).colorScheme.onSurface.withValues(alpha: 0.6),
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500),
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
