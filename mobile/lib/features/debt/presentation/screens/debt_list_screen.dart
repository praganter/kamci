import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/providers/app_settings_provider.dart';
import '../../../../core/router/route_names.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/premium_required_sheet.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../../../subscription/presentation/providers/subscription_providers.dart';
import '../../../subscription/presentation/widgets/premium_card.dart';
import '../../domain/entities/debt.dart';
import '../providers/debt_providers.dart';

class DebtListScreen extends ConsumerWidget {
  const DebtListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tabIndex = ref.watch(debtTabProvider);
    final owned = tabIndex == 0;
    return _DebtTabView(owned: owned);
  }
}

class _DebtTabView extends ConsumerWidget {
  const _DebtTabView({required this.owned});

  final bool owned;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final groupedAsync = ref.watch(groupedDebtInstancesProvider);
    final isPremium = ref.watch(isPremiumProvider);
    final selectedMonth = ref.watch(selectedMonthProvider);
    final cs = Theme.of(context).colorScheme;

    if (!owned && !isPremium) {
      return Scaffold(
        body: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  Icons.people_outline,
                  size: 64,
                  color: AppColors.statusActive,
                ),
                const SizedBox(height: 16),
                Text(
                  context.l10n.noSharedDebts,
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 12),
                Text(
                  context.l10n.sharedPremiumDesc,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 32),
                const PremiumCard(),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            // ── Month Selector ───────────────────────────────────────
            Padding(
              padding: const EdgeInsets.fromLTRB(4, 12, 4, 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Material(
                    color: Theme.of(context).colorScheme.surfaceContainerHigh,
                    elevation: 4,
                    shadowColor: Colors.black38,
                    shape: const CircleBorder(),
                    child: IconButton(
                      onPressed: () =>
                          ref.read(selectedMonthProvider.notifier).previous(),
                      icon: const Icon(Icons.chevron_left),
                    ),
                  ),
                  Material(
                    color: Theme.of(context).colorScheme.surfaceContainerHigh,
                    elevation: 4,
                    shadowColor: Colors.black38,
                    borderRadius: BorderRadius.circular(40),
                    child: GestureDetector(
                      onTap: () async {
                        final picked = await showDialog<DateTime>(
                          context: context,
                          builder: (_) =>
                              _MonthPickerDialog(initialMonth: selectedMonth),
                        );
                        if (picked != null) {
                          ref
                              .read(selectedMonthProvider.notifier)
                              .setMonth(picked);
                        }
                      },
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 10,
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              DateFormat(
                                'MMMM yyyy',
                                Localizations.localeOf(context).languageCode,
                              ).format(selectedMonth),
                              style: Theme.of(context)
                                  .textTheme
                                  .titleSmall
                                  ?.copyWith(fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(width: 4),
                            const Icon(Icons.arrow_drop_down, size: 18),
                          ],
                        ),
                      ),
                    ),
                  ),
                  Material(
                    color: Theme.of(context).colorScheme.surfaceContainerHigh,
                    elevation: 4,
                    shadowColor: Colors.black38,
                    shape: const CircleBorder(),
                    child: IconButton(
                      onPressed: () =>
                          ref.read(selectedMonthProvider.notifier).next(),
                      icon: const Icon(Icons.chevron_right),
                    ),
                  ),
                ],
              ),
            ),

            Expanded(
              child: groupedAsync.when(
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (e, _) => AppErrorWidget(
                  message: e.toString(),
                  onRetry: () => ref.invalidate(debtListProvider),
                ),
                data: (groupedByPeriod) {
                  final List<Widget> slivers = [];

                  bool hasAny = false;
                  for (final period in PaymentPeriodType.values) {
                    final categories = groupedByPeriod[period]!;
                    final filteredCategories = categories.entries
                        .where((e) => e.value.owned == owned)
                        .toList();

                    if (filteredCategories.isEmpty) continue;
                    hasAny = true;

                    slivers.add(
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                        child: Text(
                          period.l10nLabel(context.l10n).toUpperCase(),
                          style: Theme.of(context).textTheme.labelLarge
                              ?.copyWith(
                                color: cs.primary,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 1.2,
                              ),
                        ),
                      ),
                    );

                    for (final entry in filteredCategories) {
                      slivers.add(
                        _CategoryPeriodSection(
                          categoryId: entry.key,
                          categoryName: entry.value.categoryName,
                          owned: entry.value.owned,
                          instances: entry.value.instances,
                        ),
                      );
                    }
                  }

                  if (!hasAny) {
                    return RefreshIndicator(
                      onRefresh: () async => ref.invalidate(debtListProvider),
                      child: SingleChildScrollView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        child: SizedBox(
                          height: MediaQuery.of(context).size.height * 0.6,
                          child: _EmptyState(owned: owned),
                        ),
                      ),
                    );
                  }

                  return RefreshIndicator(
                    onRefresh: () async => ref.invalidate(debtListProvider),
                    child: ListView(
                      padding: const EdgeInsets.only(bottom: 88),
                      children: slivers,
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: owned
          ? FloatingActionButton(
              onPressed: () async {
                final isPremium = ref.read(isPremiumProvider);
                if (!isPremium) {
                  final debts = ref.read(debtListProvider).valueOrNull ?? [];
                  final ownedDebts = debts
                      .where(
                        (d) =>
                            d.ownerId ==
                            ref.read(currentUserIdProvider).valueOrNull,
                      )
                      .length;

                  if (ownedDebts >= 5) {
                    PremiumRequiredSheet.show(
                      context,
                      featureName: context.l10n.debts.toLowerCase(),
                      limitCount: 5,
                    );
                    return;
                  }
                }

                ref.invalidate(categoryListProvider);
                ref.invalidate(currencyListProvider);
                ref.read(currencyListProvider.future).ignore();
                await context.pushNamed(RouteNames.debtNew);
                ref.invalidate(debtListProvider);
              },
              child: const Icon(Icons.add),
            )
          : null,
    );
  }
}

class _CategoryPeriodSection extends ConsumerWidget {
  const _CategoryPeriodSection({
    required this.categoryId,
    required this.categoryName,
    required this.owned,
    required this.instances,
  });

  final String categoryId;
  final String categoryName;
  final bool owned;
  final List<DebtPaymentInstance> instances;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;

    final currenciesAsync = ref.watch(currencyListProvider);
    final currencySymbols = currenciesAsync.maybeWhen(
      data: (list) => {for (final c in list) c.code: c.symbol},
      orElse: () => <String, String>{},
    );

    final Map<String, double> totals = {};
    for (final inst in instances) {
      final code = inst.debt.currency ?? 'TRY';
      totals.update(
        code,
        (v) => v + inst.debt.amount,
        ifAbsent: () => inst.debt.amount,
      );
    }

    final amountSummary = totals.entries
        .map((e) {
          final symbol = currencySymbols[e.key] ?? e.key;
          final fmt = NumberFormat.currency(
            locale: 'tr_TR',
            symbol: symbol,
            decimalDigits: 0,
          );
          return fmt.format(e.value);
        })
        .join('  •  ');

    final paidCount = instances.where((i) => i.isPaid).length;
    final totalCount = instances.length;
    final completionRatio = totalCount > 0 ? paidCount / totalCount : 0.0;

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 4, 16, 4),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: cs.primary.withValues(alpha: 0.5),
          width: 1.5,
        ),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: ExpansionTile(
          tilePadding: const EdgeInsets.fromLTRB(12, 4, 12, 4),
          title: Text(
            categoryName,
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                amountSummary,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: cs.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 4),
              ClipRRect(
                borderRadius: BorderRadius.circular(2),
                child: LinearProgressIndicator(
                  value: completionRatio,
                  minHeight: 3,
                  backgroundColor: cs.surfaceContainerHighest,
                  valueColor: AlwaysStoppedAnimation(cs.primary),
                ),
              ),
            ],
          ),
          trailing: Container(
            padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
            decoration: BoxDecoration(
              color: cs.primaryContainer,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              '$totalCount',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: cs.onPrimaryContainer,
              ),
            ),
          ),
          children: instances
              .map((inst) => _DebtInstanceTile(instance: inst))
              .toList(),
        ),
      ),
    );
  }
}

class _DebtInstanceTile extends ConsumerWidget {
  const _DebtInstanceTile({required this.instance});
  final DebtPaymentInstance instance;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final debt = instance.debt;
    final cs = Theme.of(context).colorScheme;

    return ListTile(
      onTap: () => context.pushNamed(
        RouteNames.debtDetail,
        pathParameters: {'id': debt.id},
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: (instance.isPaid ? Colors.green : cs.primary).withValues(
            alpha: 0.1,
          ),
          shape: BoxShape.circle,
        ),
        child: Icon(
          instance.isPaid ? Icons.check_circle_outline : Icons.access_time,
          color: instance.isPaid ? Colors.green : cs.primary,
          size: 20,
        ),
      ),
      title: Text(
        debt.name,
        style: TextStyle(
          fontWeight: FontWeight.w600,
          decoration: instance.isPaid ? TextDecoration.lineThrough : null,
          color: instance.isPaid ? cs.onSurfaceVariant : cs.onSurface,
        ),
      ),
      subtitle: Text(
        DateFormat(
          'dd MMMM',
          Localizations.localeOf(context).languageCode,
        ).format(instance.paymentDate),
        style: Theme.of(context).textTheme.bodySmall,
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            '${debt.amount} ${debt.currency ?? 'TRY'}',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: instance.isPaid ? cs.onSurfaceVariant : cs.primary,
            ),
          ),
          if (debt.installmentCount != null)
            Text(
              '${instance.installmentIndex}/${debt.installmentCount}',
              style: Theme.of(context).textTheme.labelSmall,
            ),
        ],
      ),
    );
  }
}

// ── Month Picker Dialog ──────────────────────────────────────────────────────

class _MonthPickerDialog extends StatefulWidget {
  const _MonthPickerDialog({required this.initialMonth});
  final DateTime initialMonth;

  @override
  State<_MonthPickerDialog> createState() => _MonthPickerDialogState();
}

class _MonthPickerDialogState extends State<_MonthPickerDialog> {
  late int _year;
  late int _selectedMonth;

  @override
  void initState() {
    super.initState();
    _year = widget.initialMonth.year;
    _selectedMonth = widget.initialMonth.month;
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final locale = Localizations.localeOf(context).languageCode;

    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // ── Year navigation ──────────────────────────────────────
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                IconButton(
                  onPressed: () => setState(() => _year--),
                  icon: const Icon(Icons.chevron_left),
                ),
                Text(
                  '$_year',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                IconButton(
                  onPressed: () => setState(() => _year++),
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
            const SizedBox(height: 8),
            // ── Month grid ──────────────────────────────────────────
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                childAspectRatio: 2,
                mainAxisSpacing: 8,
                crossAxisSpacing: 8,
              ),
              itemCount: 12,
              itemBuilder: (context, index) {
                final month = index + 1;
                final isCurrentSelection = month == _selectedMonth;
                final label = DateFormat('MMM', locale)
                    .format(DateTime(_year, month));

                return InkWell(
                  onTap: () => Navigator.of(context).pop(DateTime(_year, month)),
                  borderRadius: BorderRadius.circular(10),
                  child: Container(
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: isCurrentSelection && _year == widget.initialMonth.year
                          ? cs.primary
                          : cs.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      label,
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: isCurrentSelection &&
                                _year == widget.initialMonth.year
                            ? cs.onPrimary
                            : cs.onSurface,
                      ),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

// ── Empty State ──────────────────────────────────────────────────────────────

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.owned});

  final bool owned;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 40),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 88,
              height: 88,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    cs.primary.withValues(alpha: 0.15),
                    cs.primary.withValues(alpha: 0.05),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                shape: BoxShape.circle,
                border: Border.all(color: cs.primary.withValues(alpha: 0.2)),
              ),
              child: Icon(
                owned
                    ? Icons.account_balance_wallet_outlined
                    : Icons.people_outline,
                size: 40,
                color: cs.primary,
              ),
            ),
            const SizedBox(height: 20),
            Text(
              owned ? l10n.noDebtsYet : l10n.noSharedDebts,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              owned ? l10n.addDebtHint : l10n.joinSharedHint,
              style: Theme.of(
                context,
              ).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
