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
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../domain/entities/category_with_debts.dart';
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
    final l10n = context.l10n;
    final groupedAsync = ref.watch(debtGroupedProvider);

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
          final filtered = groups.where((g) => g.owned == owned).toList();

          if (filtered.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      owned
                          ? Icons.account_balance_wallet_outlined
                          : Icons.people_outline,
                      size: 32,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    owned ? l10n.noDebtsYet : l10n.noSharedDebts,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    owned ? l10n.addDebtHint : l10n.joinSharedHint,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.read(debtGroupedProvider.notifier).refresh(),
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(vertical: 8),
              itemCount: filtered.length,
              itemBuilder: (context, index) => _CategorySection(
                group: filtered[index],
              ),
            ),
          );
        },
        ),
      ),
      floatingActionButton: owned
          ? FloatingActionButton(
              onPressed: () => context.pushNamed(RouteNames.debtNew),
              child: const Icon(Icons.add),
            )
          : null,
    );
  }
}

class _CategorySection extends ConsumerWidget {
  const _CategorySection({required this.group});

  final CategoryWithDebts group;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final currencyFormat = NumberFormat.currency(
      locale: 'tr_TR',
      symbol: '₺',
      decimalDigits: 0,
    );
    final cs = Theme.of(context).colorScheme;

    final Map<PaymentPeriodType, double> totals = {};
    for (final debt in group.debts) {
      totals.update(
        debt.paymentPeriodType,
        (v) => v + debt.amount,
        ifAbsent: () => debt.amount,
      );
    }
    final summary = totals.entries
        .map((e) => '${e.key.l10nLabel(l10n)}: ${currencyFormat.format(e.value)}')
        .join('  •  ');

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 4, 16, 4),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: cs.outlineVariant),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: ExpansionTile(
          title: Text(
            group.categoryName,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
          ),
          subtitle: group.debts.isEmpty
              ? Text(l10n.noDebts,
                  style: Theme.of(context).textTheme.bodySmall)
              : Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Text(
                    summary,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: cs.primary,
                          fontWeight: FontWeight.w500,
                        ),
                  ),
                ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  '${group.debts.length}',
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
              ...group.debts.map((debt) => _CompactDebtTile(debt: debt)),
            ],
          ],
        ),
      ),
    );
  }
}

class _CompactDebtTile extends ConsumerWidget {
  const _CompactDebtTile({required this.debt});

  final Debt debt;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final currencyFormat = NumberFormat.currency(
      locale: 'tr_TR',
      symbol: '₺',
      decimalDigits: 0,
    );
    final cs = Theme.of(context).colorScheme;
    final statusColor = switch (debt.status) {
      DebtStatus.active => AppColors.statusActive,
      DebtStatus.inactive => AppColors.statusInactive,
      DebtStatus.completed => cs.primary,
    };

    return InkWell(
      onTap: () => context.pushNamed(
        RouteNames.debtDetail,
        pathParameters: {'id': debt.id},
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                color: statusColor,
                shape: BoxShape.circle,
                boxShadow: debt.status == DebtStatus.active
                    ? [
                        BoxShadow(
                          color: statusColor.withValues(alpha: 0.5),
                          blurRadius: 6,
                          spreadRadius: 1,
                        )
                      ]
                    : null,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    debt.name,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w500,
                        ),
                  ),
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      Text(
                        currencyFormat.format(debt.amount),
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: cs.primary,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                      Text(
                        '  •  ${debt.paymentPeriodType.l10nLabel(l10n)}',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ],
              ),
            ),
            PopupMenuButton(
              icon: Icon(Icons.more_vert, size: 18, color: cs.onSurfaceVariant),
              itemBuilder: (_) => [
                PopupMenuItem(value: 'edit', child: Text(l10n.edit)),
                PopupMenuItem(
                  value: 'delete',
                  child: Text(l10n.delete,
                      style: const TextStyle(color: Colors.red)),
                ),
              ],
              onSelected: (v) async {
                if (v == 'edit') {
                  context.pushNamed(
                    RouteNames.debtEdit,
                    pathParameters: {'id': debt.id},
                  );
                } else if (v == 'delete') {
                  final confirmed = await showConfirmDialog(
                    context,
                    title: l10n.deleteDebt,
                    content: l10n.deleteDebtConfirm(debt.name),
                  );
                  if (confirmed && context.mounted) {
                    await ref
                        .read(debtListProvider.notifier)
                        .deleteDebt(debt.id);
                    ref.invalidate(debtGroupedProvider);
                  }
                }
              },
            ),
          ],
        ),
      ),
    );
  }
}
