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
    final groupedAsync = ref.watch(debtGroupedProvider);
    final statusFilter = ref.watch(debtStatusFilterProvider);

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
                .where((g) => g.debts.isNotEmpty)
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
                  return _CategorySection(group: filtered[index - 1]);
                },
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

// ── Katman 1: Özet Header ────────────────────────────────────────────────────

class _DebtSummaryHeader extends StatelessWidget {
  const _DebtSummaryHeader({required this.groups});

  final List<CategoryWithDebts> groups;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final currencyFormat = NumberFormat.currency(
      locale: 'tr_TR',
      symbol: '₺',
      decimalDigits: 0,
    );

    double activeAmount = 0;
    double inactiveAmount = 0;
    double totalAmount = 0;

    for (final group in groups) {
      for (final debt in group.debts) {
        totalAmount += debt.amount;
        if (debt.status == DebtStatus.active) activeAmount += debt.amount;
        if (debt.status == DebtStatus.passive) inactiveAmount += debt.amount;
      }
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
      child: Row(
        children: [
          Expanded(
            child: _AmountColumn(
              label: l10n.statusActive,
              amount: currencyFormat.format(activeAmount),
              color: AppColors.statusActive,
            ),
          ),
          Container(
            width: 1,
            height: 40,
            margin: const EdgeInsets.symmetric(horizontal: 12),
            color: cs.primary.withValues(alpha: 0.2),
          ),
          Expanded(
            child: _AmountColumn(
              label: l10n.statusInactive,
              amount: currencyFormat.format(inactiveAmount),
              color: AppColors.statusInactive,
            ),
          ),
          Container(
            width: 1,
            height: 40,
            margin: const EdgeInsets.symmetric(horizontal: 12),
            color: cs.primary.withValues(alpha: 0.2),
          ),
          Expanded(
            child: _AmountColumn(
              label: l10n.summaryTotal,
              amount: currencyFormat.format(totalAmount),
              color: cs.primary,
            ),
          ),
        ],
      ),
    );
  }
}

class _AmountColumn extends StatelessWidget {
  const _AmountColumn({
    required this.label,
    required this.amount,
    required this.color,
  });

  final String label;
  final String amount;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
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
          amount,
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

    final activeCount =
        group.debts.where((d) => d.status == DebtStatus.active).length;
    final inactiveCount =
        group.debts.where((d) => d.status == DebtStatus.passive).length;
    final total = group.debts.length;
    final completionRatio = total > 0 ? inactiveCount / total : 0.0;

    final Map<PaymentPeriodType, double> totals = {};
    for (final debt in group.debts) {
      totals.update(
        debt.paymentPeriodType,
        (v) => v + debt.amount,
        ifAbsent: () => debt.amount,
      );
    }
    final amountSummary = totals.entries
        .map((e) => '${e.key.l10nLabel(l10n)}: ${currencyFormat.format(e.value)}')
        .join('  •  ');

    return Container(
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
              ...group.debts.map((debt) => _SwipeableTile(debt: debt)),
            ],
          ],
        ),
      ),
    );
  }
}

// ── Katman 3: Swipeable wrapper ───────────────────────────────────────────────

class _SwipeableTile extends ConsumerStatefulWidget {
  const _SwipeableTile({required this.debt});
  final Debt debt;

  @override
  ConsumerState<_SwipeableTile> createState() => _SwipeableTileState();
}

class _SwipeableTileState extends ConsumerState<_SwipeableTile>
    with SingleTickerProviderStateMixin {
  static const double _actionWidth = 72.0;
  double _dragOffset = 0;

  late final AnimationController _snapCtrl;
  late final Animation<double> _snapAnim;
  double _snapFrom = 0;

  @override
  void initState() {
    super.initState();
    _snapCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
    _snapAnim = CurvedAnimation(parent: _snapCtrl, curve: Curves.easeOut);
    _snapCtrl.addListener(() {
      setState(() {
        _dragOffset = _snapFrom * (1 - _snapAnim.value);
      });
    });
  }

  @override
  void dispose() {
    _snapCtrl.dispose();
    super.dispose();
  }

  void _snapBack() {
    _snapFrom = _dragOffset;
    _snapCtrl.forward(from: 0);
  }

  Future<void> _onDeleteTap() async {
    final confirmed = await showConfirmDialog(
      context,
      title: context.l10n.deleteDebt,
      content: context.l10n.deleteDebtPermanentWarning,
    );
    if (!mounted) return;
    if (confirmed) {
      await ref.read(debtListProvider.notifier).deleteDebt(widget.debt.id);
      ref.invalidate(debtGroupedProvider);
    } else {
      _snapBack();
    }
  }

  Future<void> _onPaymentTap() async {
    _snapBack();
    try {
      await ref.read(debtListProvider.notifier).makePayment(widget.debt.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle_rounded,
                    color: Colors.white, size: 20),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    context.l10n.paymentRecorded(widget.debt.name),
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            backgroundColor: AppColors.statusActive,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12)),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString()),
            backgroundColor: const Color(0xFFEF4444),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12)),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final showPayment = _dragOffset > 0;
    final showDelete = _dragOffset < 0;

    return GestureDetector(
      onHorizontalDragUpdate: (details) {
        setState(() {
          _dragOffset = (_dragOffset + details.delta.dx)
              .clamp(-_actionWidth, _actionWidth);
        });
      },
      onHorizontalDragEnd: (_) {
        if (_dragOffset.abs() < _actionWidth / 2) _snapBack();
      },
      child: Stack(
        children: [
          // Payment action (sağa sürükle — soldan çıkar)
          if (showPayment)
            Positioned(
              left: 0,
              top: 0,
              bottom: 0,
              width: _dragOffset,
              child: GestureDetector(
                onTap: _onPaymentTap,
                child: Container(
                  color: AppColors.statusActive,
                  child: const Center(
                    child: Icon(Icons.payments_rounded,
                        color: Colors.white, size: 26),
                  ),
                ),
              ),
            ),
          // Delete action (sola sürükle — sağdan çıkar)
          if (showDelete)
            Positioned(
              right: 0,
              top: 0,
              bottom: 0,
              width: -_dragOffset,
              child: GestureDetector(
                onTap: _onDeleteTap,
                child: Container(
                  color: const Color(0xFFEF4444),
                  child: const Center(
                    child: Icon(Icons.delete_rounded,
                        color: Colors.white, size: 26),
                  ),
                ),
              ),
            ),
          // Tile içeriği
          Transform.translate(
            offset: Offset(_dragOffset, 0),
            child: _CompactDebtTile(debt: widget.debt),
          ),
        ],
      ),
    );
  }
}

// ── Katman 3: Borç Satırı ────────────────────────────────────────────────────

class _CompactDebtTile extends StatelessWidget {
  const _CompactDebtTile({required this.debt});

  final Debt debt;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final currencyFormat = NumberFormat.currency(
      locale: 'tr_TR',
      symbol: '₺',
      decimalDigits: 0,
    );
    final cs = Theme.of(context).colorScheme;

    final statusColor = switch (debt.status) {
      DebtStatus.active => AppColors.statusActive,
      DebtStatus.passive => AppColors.statusInactive,
    };
    final statusLabel = switch (debt.status) {
      DebtStatus.active => l10n.statusActive,
      DebtStatus.passive => l10n.statusInactive,
    };
    final statusIcon = switch (debt.status) {
      DebtStatus.active => Icons.radio_button_checked_rounded,
      DebtStatus.passive => Icons.pause_circle_outline_rounded,
    };

    final paymentDotColor = switch (debt.status) {
      DebtStatus.passive => AppColors.statusInactive,
      DebtStatus.active => debt.paidCurrentPeriod
          ? AppColors.statusActive
          : const Color(0xFFEF4444),
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
                        )
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
                  Row(
                    children: [
                      // Status chip
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 7, vertical: 3),
                        decoration: BoxDecoration(
                          color: statusColor.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                              color: statusColor.withValues(alpha: 0.3)),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(statusIcon, size: 10, color: statusColor),
                            const SizedBox(width: 3),
                            Text(
                              statusLabel,
                              style: Theme.of(context)
                                  .textTheme
                                  .labelSmall
                                  ?.copyWith(
                                    color: statusColor,
                                    fontWeight: FontWeight.w600,
                                  ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 6),
                      // Period chip
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 7, vertical: 3),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: cs.outlineVariant),
                        ),
                        child: Text(
                          debt.paymentPeriodType.l10nLabel(l10n),
                          style: Theme.of(context)
                              .textTheme
                              .labelSmall
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
            Text(
              currencyFormat.format(debt.amount),
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: cs.primary,
                    fontWeight: FontWeight.w700,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}
