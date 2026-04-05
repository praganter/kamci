import 'dart:ui';
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
import '../../../category/presentation/providers/category_providers.dart';
import '../../../subscription/presentation/providers/subscription_providers.dart';
import '../../../subscription/presentation/widgets/premium_card.dart';
import '../../domain/entities/category_with_debts.dart';
import '../../domain/entities/debt.dart';
import '../providers/debt_providers.dart';
import '../widgets/category_management_sheet.dart';
import '../widgets/swipeable_debt_tile.dart';

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
    final groupedAsync = ref.watch(debtGroupedProvider);
    final statusFilter = ref.watch(debtStatusFilterProvider);
    final isPremium = ref.watch(isPremiumProvider);

    // If "Shared" tab and not premium, show premium card
    if (!owned && !isPremium) {
      return Scaffold(
        body: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.people_outline, size: 64, color: AppColors.statusActive),
                const SizedBox(height: 16),
                Text(
                  context.l10n.noSharedDebts,
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 12),
                Text(
                  context.l10n.sharedPremiumDesc,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
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
        child: groupedAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => AppErrorWidget(
            message: e.toString(),
            onRetry: () => ref.invalidate(debtGroupedProvider),
          ),
          data: (groups) {
            final filtered = groups
                .where((g) => g.owned == owned)
                .map((g) {
                  final debts = switch (statusFilter) {
                    DebtStatusFilter.all => g.debts,
                    DebtStatusFilter.active => g.debts
                        .where((d) => d.status == DebtStatus.active)
                        .toList(),
                    DebtStatusFilter.passive => g.debts
                        .where((d) => d.status == DebtStatus.passive)
                        .toList(),
                  };
                  return CategoryWithDebts(
                    categoryId: g.categoryId,
                    categoryName: g.categoryName,
                    owned: g.owned,
                    debts: debts,
                  );
                })
                .toList();

            if (filtered.isEmpty) {
              return _EmptyState(owned: owned);
            }

            return RefreshIndicator(
              onRefresh: () => ref.read(debtGroupedProvider.notifier).refresh(),
              child: ListView.builder(
                padding: const EdgeInsets.only(bottom: 88),
                itemCount: filtered.length + 1,
                itemBuilder: (context, index) {
                  if (index == 0) {
                    return _DebtSummaryHeader(groups: filtered);
                  }
                  
                  final categoryIndex = index - 1;
                  // If not premium and it's the 3rd category (index 2) or beyond
                  final isLocked = !isPremium && categoryIndex >= 2;
                  
                  return _CategorySection(
                    group: filtered[categoryIndex],
                    isLocked: isLocked,
                  );
                },
              ),
            );
          },
        ),
      ),
      floatingActionButton: owned
          ? FloatingActionButton(
              onPressed: () async {
                final isPremium = ref.read(isPremiumProvider);
                if (!isPremium) {
                  final groups = groupedAsync.valueOrNull ?? [];
                  final totalDebts = groups
                      .where((g) => g.owned)
                      .fold(0, (sum, g) => sum + g.debts.length);

                  if (totalDebts >= 5) {
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
                ref.invalidate(debtGroupedProvider);
              },
              child: const Icon(Icons.add),
            )
          : null,
    );
  }
}

// ── Katman 1: Özet Header ────────────────────────────────────────────────────

class _DebtSummaryHeader extends ConsumerWidget {
  const _DebtSummaryHeader({required this.groups});

  final List<CategoryWithDebts> groups;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final currenciesAsync = ref.watch(currencyListProvider);
    final currencySymbols = currenciesAsync.maybeWhen(
      data: (list) => {for (final c in list) c.code: c.symbol},
      orElse: () => <String, String>{},
    );

    final Map<String, double> activeAmounts = {};
    final Map<String, double> inactiveAmounts = {};

    for (final group in groups) {
      for (final debt in group.debts) {
        final code = debt.currency ?? 'TRY';
        if (debt.status == DebtStatus.active) {
          activeAmounts.update(code, (v) => v + debt.amount,
              ifAbsent: () => debt.amount);
        } else {
          inactiveAmounts.update(code, (v) => v + debt.amount,
              ifAbsent: () => debt.amount);
        }
      }
    }

    final allCodes = {...activeAmounts.keys, ...inactiveAmounts.keys};
    final Map<String, double> totalAmounts = {
      for (final code in allCodes)
        code: (activeAmounts[code] ?? 0) + (inactiveAmounts[code] ?? 0),
    };

    String formatAmounts(Map<String, double> amounts) {
      if (amounts.isEmpty) return '—';
      return amounts.entries.map((e) {
        final symbol = currencySymbols[e.key] ?? e.key;
        return NumberFormat.currency(
                locale: 'tr_TR', symbol: symbol, decimalDigits: 0)
            .format(e.value);
      }).join('\n');
    }

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 18),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: isDark
              ? [const Color(0xFF1C0F30), const Color(0xFF2A1450)]
              : [const Color(0xFFEDE7FF), const Color(0xFFD8C8FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: cs.primary.withValues(alpha: isDark ? 0.25 : 0.12),
        ),
      ),
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: _AmountColumn(
                label: l10n.statusActive,
                amounts: formatAmounts(activeAmounts),
                color: AppColors.statusActive,
              ),
            ),
            Container(
              width: 1,
              margin: const EdgeInsets.symmetric(horizontal: 12),
              color: cs.primary.withValues(alpha: 0.2),
            ),
            Expanded(
              child: _AmountColumn(
                label: l10n.statusInactive,
                amounts: formatAmounts(inactiveAmounts),
                color: AppColors.statusInactive,
              ),
            ),
            Container(
              width: 1,
              margin: const EdgeInsets.symmetric(horizontal: 12),
              color: cs.primary.withValues(alpha: 0.2),
            ),
            Expanded(
              child: _AmountColumn(
                label: l10n.summaryTotal,
                amounts: formatAmounts(totalAmounts),
                color: cs.primary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AmountColumn extends StatelessWidget {
  const _AmountColumn({
    required this.label,
    required this.amounts,
    required this.color,
  });

  final String label;
  final String amounts;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 6,
              height: 6,
              decoration: BoxDecoration(shape: BoxShape.circle, color: color),
            ),
            const SizedBox(width: 5),
            Text(
              label,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: color,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.3,
                  ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          amounts,
          style: Theme.of(context).textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w800,
                color: cs.onSurface,
                letterSpacing: -0.3,
              ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}

class _StatusCountRow extends StatelessWidget {
  const _StatusCountRow({
    required this.count,
    required this.label,
    required this.color,
  });

  final int count;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 6,
          height: 6,
          decoration: BoxDecoration(shape: BoxShape.circle, color: color),
        ),
        const SizedBox(width: 4),
        Text(
          '$count $label',
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
                color: color,
                fontWeight: FontWeight.w600,
              ),
        ),
      ],
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
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              owned ? l10n.addDebtHint : l10n.joinSharedHint,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: cs.onSurfaceVariant,
                  ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

// ── Katman 2: Kategori Kartı ─────────────────────────────────────────────────

class _CategorySection extends ConsumerWidget {
  const _CategorySection({
    required this.group,
    this.isLocked = false,
  });

  final CategoryWithDebts group;
  final bool isLocked;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    final activeCount =
        group.debts.where((d) => d.status == DebtStatus.active).length;
    final inactiveCount =
        group.debts.where((d) => d.status == DebtStatus.passive).length;
    final total = group.debts.length;
    final completionRatio = total > 0 ? inactiveCount / total : 0.0;

    final currenciesAsync = ref.watch(currencyListProvider);
    final currencySymbols = currenciesAsync.maybeWhen(
      data: (list) => {for (final c in list) c.code: c.symbol},
      orElse: () => <String, String>{},
    );

    final Map<(PaymentPeriodType, String), double> totals = {};
    for (final debt in group.debts) {
      final key = (debt.paymentPeriodType, debt.currency ?? 'TRY');
      totals.update(key, (v) => v + debt.amount, ifAbsent: () => debt.amount);
    }
    final amountSummary = totals.entries.map((e) {
      final symbol = currencySymbols[e.key.$2] ?? e.key.$2;
      final fmt = NumberFormat.currency(
          locale: 'tr_TR', symbol: symbol, decimalDigits: 0);
      return '${e.key.$1.l10nLabel(l10n)}: ${fmt.format(e.value)}';
    }).join('  •  ');

    Widget body = GestureDetector(
      onLongPress: isLocked ? null : () => showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        useSafeArea: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        builder: (_) => CategoryManagementSheet(
          categoryId: group.categoryId,
          categoryName: group.categoryName,
          owned: group.owned,
        ),
      ),
      child: Container(
      margin: const EdgeInsets.fromLTRB(16, 4, 16, 4),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: cs.primary, width: 2),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: ExpansionTile(
          tilePadding: const EdgeInsets.fromLTRB(12, 4, 12, 4),
          enabled: !isLocked,
          title: Text(
            group.categoryName,
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
          ),
          subtitle: Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (group.debts.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    amountSummary,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: cs.primary,
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                  const SizedBox(height: 6),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: LinearProgressIndicator(
                      value: completionRatio,
                      minHeight: 3,
                      backgroundColor: cs.surfaceContainerHighest,
                      valueColor: AlwaysStoppedAnimation(cs.primary),
                    ),
                  ),
                ] else
                  Text(l10n.noDebts,
                      style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  if (activeCount > 0)
                    _StatusCountRow(
                      count: activeCount,
                      label: l10n.statusActive,
                      color: AppColors.statusActive,
                    ),
                  if (inactiveCount > 0)
                    _StatusCountRow(
                      count: inactiveCount,
                      label: l10n.statusInactive,
                      color: AppColors.statusInactive,
                    ),
                ],
              ),
              const SizedBox(width: 8),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  '$total',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: cs.onPrimaryContainer,
                  ),
                ),
              ),
              const SizedBox(width: 2),
            ],
          ),
          collapsedIconColor: cs.onSurfaceVariant,
          iconColor: cs.primary,
          initiallyExpanded: false,
          children: [
            if (group.debts.isEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
                child: Text(
                  l10n.noCategoryDebts,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              )
            else ...[
              Divider(height: 1, color: cs.outlineVariant),
              ...group.debts.map((debt) => SwipeableTile(debt: debt)),
            ],
          ],
        ),
      ),
    ),
    );

    if (isLocked) {
      return Stack(
        children: [
          body,
          Positioned.fill(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: BackdropFilter(
                filter: ImageFilter.blur(sigmaX: 4, sigmaY: 4),
                child: Container(
                  margin: const EdgeInsets.fromLTRB(16, 4, 16, 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Center(
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: cs.surface.withValues(alpha: 0.8),
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.1),
                            blurRadius: 8,
                            spreadRadius: 2,
                          ),
                        ],
                      ),
                      child: Icon(Icons.lock_outline, color: cs.primary, size: 24),
                    ),
                  ),
                ),
              ),
            ),
          ),
          Positioned.fill(
            child: GestureDetector(
              onTap: () => PremiumRequiredSheet.show(
                context,
                featureName: 'Kategori',
                limitCount: 2,
              ),
              child: Container(color: Colors.transparent),
            ),
          ),
        ],
      );
    }

    return body;
  }
}

