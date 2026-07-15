import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/router/route_names.dart';
import '../../../../core/utils/permission_utils.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/snackbar_utils.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../../../category/domain/entities/member_embedded.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../../shared/widgets/premium_required_sheet.dart';
import '../../../../shared/widgets/pro_badge.dart';
import '../../../category/domain/entities/category.dart';
import '../../../category/presentation/widgets/add_category_sheet.dart';
import '../../../subscription/presentation/providers/subscription_providers.dart';
import '../../../subscription/presentation/widgets/premium_card.dart';
import '../../../subscription/subscription_constants.dart';
import '../../../../core/providers/app_settings_provider.dart';
import '../../domain/entities/bulk_payment_entry.dart';
import '../../domain/entities/debt.dart';
import '../providers/debt_providers.dart';

class DebtListScreen extends ConsumerStatefulWidget {
  const DebtListScreen({super.key});

  @override
  ConsumerState<DebtListScreen> createState() => _DebtListScreenState();
}

class _DebtListScreenState extends ConsumerState<DebtListScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(_onTabChanged);
  }

  void _onTabChanged() {
    if (!_tabController.indexIsChanging) setState(() {});
  }

  @override
  void dispose() {
    _tabController.removeListener(_onTabChanged);
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final l10n = context.l10n;
    final isPremium = ref.watch(isPremiumProvider);
    // Keep categoryListProvider alive while this screen is in the stack.
    // This prevents auto-dispose between FAB pre-fetch and form opening.
    ref.watch(categoryListProvider);

    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
            child: Container(
              height: 40,
              decoration: BoxDecoration(
                color: cs.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(20),
              ),
              child: TabBar(
                controller: _tabController,
                indicatorSize: TabBarIndicatorSize.tab,
                indicator: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                dividerColor: Colors.transparent,
                labelColor: cs.primary,
                unselectedLabelColor: cs.onSurfaceVariant,
                labelStyle: const TextStyle(fontWeight: FontWeight.w600),
                overlayColor: WidgetStateProperty.all(Colors.transparent),
                tabs: [
                  Semantics(
                    identifier: 'myDebtsTab',
                    child: Tab(text: l10n.myDebts),
                  ),
                  Semantics(
                    identifier: 'sharedDebtsTab',
                    child: Tab(text: l10n.sharedDebts),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [_DebtTabView(owned: true), _DebtTabView(owned: false)],
      ),
      floatingActionButton: _tabController.index == 0
          ? Semantics(
              identifier: 'addDebtFab',
              child: FloatingActionButton(
                onPressed: () async {
                  if (!isPremium) {
                    final debts =
                        ref.read(debtListProvider).valueOrNull ?? [];
                    if (debts.length >=
                        SubscriptionConstants.freeDebtLimit) {
                      PremiumRequiredSheet.show(
                        context,
                        featureName: l10n.debts.toLowerCase(),
                        limitCount: SubscriptionConstants.freeDebtLimit,
                      );
                      return;
                    }
                  }
                  ref.invalidate(currencyListProvider);
                  await context.pushNamed(RouteNames.debtNew);
                },
                child: const Icon(Icons.add),
              ),
            )
          : null,
    );
  }
}

class _DebtTabView extends ConsumerStatefulWidget {
  const _DebtTabView({required this.owned});

  final bool owned;

  @override
  ConsumerState<_DebtTabView> createState() => _DebtTabViewState();
}

class _DebtTabViewState extends ConsumerState<_DebtTabView>
    with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final owned = widget.owned;
    final groupedAsync = ref.watch(groupedDebtInstancesByOwnerProvider(owned));
    final isPremium = ref.watch(isPremiumProvider);
    final selectedMonth = ref.watch(selectedMonthProvider);
    final cs = Theme.of(context).colorScheme;

    return SafeArea(
      bottom: false,
      child: Column(
        children: [
          // ── Month Selector ───────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Container(
              decoration: BoxDecoration(
                color: cs.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                children: [
                  Semantics(
                    identifier: 'prevMonthBtn',
                    child: IconButton(
                      key: const Key('prevMonthBtn'),
                      onPressed: () =>
                          ref.read(selectedMonthProvider.notifier).previous(),
                      icon: Icon(Icons.chevron_left, color: cs.onSurfaceVariant),
                    ),
                  ),
                  Expanded(
                    child: InkWell(
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
                      borderRadius: BorderRadius.circular(12),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              DateFormat(
                                'MMMM yyyy',
                                Localizations.localeOf(context).languageCode,
                              ).format(selectedMonth),
                              style: Theme.of(context).textTheme.titleSmall
                                  ?.copyWith(fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(width: 4),
                            Icon(
                              Icons.keyboard_arrow_down_rounded,
                              size: 18,
                              color: cs.onSurfaceVariant,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  Semantics(
                    identifier: 'nextMonthBtn',
                    child: IconButton(
                      key: const Key('nextMonthBtn'),
                      onPressed: () =>
                          ref.read(selectedMonthProvider.notifier).next(),
                      icon: Icon(Icons.chevron_right, color: cs.onSurfaceVariant),
                    ),
                  ),
                ],
              ),
            ),
          ),

          Expanded(
            child: (!owned && !isPremium)
                ? Center(
                    child: SingleChildScrollView(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.people_outline,
                            size: 64,
                            color: cs.primary,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            context.l10n.noSharedDebts,
                            style: Theme.of(context).textTheme.titleLarge
                                ?.copyWith(fontWeight: FontWeight.bold),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 12),
                          Text(
                            context.l10n.sharedPremiumDesc,
                            style: Theme.of(context).textTheme.bodyMedium
                                ?.copyWith(color: cs.onSurfaceVariant),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 32),
                          const PremiumCard(),
                        ],
                      ),
                    ),
                  )
                : groupedAsync.when(
                    loading: () =>
                        const Center(child: CircularProgressIndicator()),
                    error: (e, _) => AppErrorWidget(
                      error: e,
                      onRetry: () {
                        ref.invalidate(debtGroupedProvider(true));
                        ref.invalidate(debtGroupedProvider(false));
                      },
                    ),
                    data: (groupedByPeriod) {
                      final List<Widget> slivers = [];

                      bool hasAny = false;
                      for (final period in PaymentPeriodType.values) {
                        final categories = groupedByPeriod[period]!;
                        final entries = categories.entries.toList();

                        if (entries.isEmpty) continue;
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

                        for (final entry in entries) {
                          slivers.add(
                            _CategoryPeriodSection(
                              categoryId: entry.key,
                              categoryName: entry.value.categoryName,
                              owned: entry.value.owned,
                              items: entry.value.items,
                            ),
                          );
                        }
                      }

                      if (!hasAny) {
                        return RefreshIndicator(
                          onRefresh: () async {
                            ref.invalidate(debtGroupedProvider(true));
                            ref.invalidate(debtGroupedProvider(false));
                          },
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
                        onRefresh: () async {
                          ref.invalidate(debtGroupedProvider(true));
                          ref.invalidate(debtGroupedProvider(false));
                        },
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
    );
  }
}

class _CategoryPeriodSection extends ConsumerWidget {
  const _CategoryPeriodSection({
    required this.categoryId,
    required this.categoryName,
    required this.owned,
    required this.items,
  });

  final String categoryId;
  final String categoryName;
  final bool owned;
  final List<DebtListItem> items;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;

    final currenciesAsync = ref.watch(currencyListProvider);
    final currencySymbols = currenciesAsync.maybeWhen(
      data: (list) => {for (final c in list) c.code: c.symbol},
      orElse: () => <String, String>{},
    );

    final baseCurrency = ref.watch(baseCurrencyProvider);
    final ratesAsync = ref.watch(exchangeRatesProvider);

    final Map<String, double> totals = {};
    for (final item in items) {
      final code = item.debt.currency;
      totals.update(
        code,
        (v) => v + item.debt.amount,
        ifAbsent: () => item.debt.amount,
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

    String? convertedTotal;
    final rates = ratesAsync.valueOrNull;
    if (rates != null && totals.length > 1) {
      double total = 0;
      bool canConvert = true;
      for (final e in totals.entries) {
        if (e.key == baseCurrency) {
          total += e.value;
        } else {
          final rate = rates[e.key];
          if (rate != null && rate > 0) {
            total += e.value / rate;
          } else {
            canConvert = false;
            break;
          }
        }
      }
      if (canConvert) {
        final baseSymbol = currencySymbols[baseCurrency] ?? baseCurrency;
        final fmt = NumberFormat.currency(
          locale: 'tr_TR',
          symbol: baseSymbol,
          decimalDigits: 0,
        );
        convertedTotal = '≈ ${fmt.format(total)}';
      }
    }

    final paidCount = items.where((i) => i.debt.paidCurrentPeriod).length;
    final totalCount = items.length;
    final completionRatio = totalCount > 0 ? paidCount / totalCount : 0.0;

    return GestureDetector(
      onLongPressStart: (details) =>
          _showCategoryMenu(context, ref, details.globalPosition),
      child: Container(
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
            initiallyExpanded: true,
            tilePadding: const EdgeInsets.fromLTRB(12, 4, 12, 4),
            backgroundColor: cs.primary.withValues(alpha: 0.04),
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
                if (convertedTotal != null)
                  Text(
                    convertedTotal,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: cs.onSurfaceVariant,
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
            children: [
              Divider(
                height: 1,
                thickness: 1,
                color: cs.primary.withValues(alpha: 0.2),
              ),
              for (int i = 0; i < items.length; i++) ...[
                _DebtInstanceTile(
                  item: items[i],
                  isLast: i == items.length - 1,
                ),
                if (i < items.length - 1)
                  Divider(
                    height: 1,
                    thickness: 0.5,
                    indent: 52,
                    endIndent: 16,
                    color: cs.outlineVariant.withValues(alpha: 0.5),
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showCategoryMenu(
    BuildContext context,
    WidgetRef ref,
    Offset globalPosition,
  ) async {
    final l10n = context.l10n;
    final overlay =
        Overlay.of(context).context.findRenderObject()! as RenderBox;
    final result = await showMenu<String>(
      context: context,
      position: RelativeRect.fromRect(
        globalPosition & const Size(1, 1),
        Offset.zero & overlay.size,
      ),
      items: [
        PopupMenuItem(
          value: 'edit',
          child: Row(
            children: [
              const Icon(Icons.edit_outlined, size: 18),
              const SizedBox(width: 10),
              Text(l10n.editCategory),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'transfer',
          child: Row(
            children: [
              const Icon(Icons.swap_horiz_rounded, size: 18),
              const SizedBox(width: 10),
              Text(l10n.transferAndDeleteCategory),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'delete',
          child: Row(
            children: [
              Icon(Icons.delete_outline, size: 18, color: Colors.red.shade400),
              const SizedBox(width: 10),
              Text(
                l10n.deleteWithDebts,
                style: TextStyle(color: Colors.red.shade400),
              ),
            ],
          ),
        ),
      ],
    );
    if (result == null || !context.mounted) return;

    final accessLevels = ref.read(categoryAccessLevelsProvider).valueOrNull;
    final level = accessLevels?[categoryId];

    switch (result) {
      case 'edit':
        final hasEdit = PermissionUtils.check(
          context,
          isOwner: owned,
          level: level,
          action: (l) => l.canEdit,
        );
        if (!hasEdit) return;

        showModalBottomSheet(
          context: context,
          isScrollControlled: true,
          useSafeArea: true,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
          builder: (_) => AddCategorySheet(
            categoryId: categoryId,
            initialName: categoryName,
          ),
        );
      case 'transfer':
        final hasDelete = PermissionUtils.check(
          context,
          isOwner: owned,
          level: level,
          action: (l) => l.canDelete,
        );
        if (!hasDelete) return;

        if (context.mounted) _showTransferDialog(context, ref);
      case 'delete':
        final hasDelete = PermissionUtils.check(
          context,
          isOwner: owned,
          level: level,
          action: (l) => l.canDelete,
        );
        if (!hasDelete) return;

        if (context.mounted) _confirmDeleteWithDebts(context, ref);
    }
  }

  Future<void> _showTransferDialog(BuildContext context, WidgetRef ref) async {
    final categoriesAsync = ref.read(categoryListProvider);
    final categories = categoriesAsync.valueOrNull ?? [];
    final targets = categories.where((c) => c.id != categoryId).toList();

    if (targets.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(context.l10n.noCategoriesYet)));
      return;
    }

    final targetId = await showDialog<String>(
      context: context,
      builder: (ctx) =>
          _TransferCategoryDialog(sourceName: categoryName, targets: targets),
    );
    if (targetId == null || !context.mounted) return;

    try {
      final count = await ref
          .read(categoryListProvider.notifier)
          .transferDebtsAndDelete(categoryId, targetId);
      ref.invalidate(debtGroupedProvider(true));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.migratedDebtCount(count))),
        );
      }
    } catch (e) {
      if (context.mounted) showErrorSnackBar(context, e);
    }
  }

  Future<void> _confirmDeleteWithDebts(
    BuildContext context,
    WidgetRef ref,
  ) async {
    final l10n = context.l10n;
    final confirmed = await showConfirmDialog(
      context,
      title: l10n.deleteWithDebts,
      content: l10n.deleteWithDebtsConfirm(categoryName),
    );
    if (!confirmed || !context.mounted) return;
    try {
      await ref.read(categoryListProvider.notifier).deleteCategory(categoryId);
      ref.invalidate(debtGroupedProvider(true));
    } catch (e) {
      if (context.mounted) showErrorSnackBar(context, e);
    }
  }
}

class _TransferCategoryDialog extends StatefulWidget {
  const _TransferCategoryDialog({
    required this.sourceName,
    required this.targets,
  });

  final String sourceName;
  final List<Category> targets;

  @override
  State<_TransferCategoryDialog> createState() =>
      _TransferCategoryDialogState();
}

class _TransferCategoryDialogState extends State<_TransferCategoryDialog> {
  String? _selectedId;

  @override
  void initState() {
    super.initState();
    _selectedId = widget.targets.first.id;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AlertDialog(
      title: Text(l10n.transferAndDeleteCategory),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(l10n.transferAndDeleteConfirm(widget.sourceName)),
          const SizedBox(height: 16),
          Text(
            l10n.selectTargetCategory,
            style: Theme.of(context).textTheme.labelMedium,
          ),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _selectedId,
            isExpanded: true,
            decoration: const InputDecoration(
              contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              border: OutlineInputBorder(),
            ),
            items: widget.targets
                .map((c) => DropdownMenuItem(value: c.id, child: Text(c.name)))
                .toList(),
            onChanged: (v) => setState(() => _selectedId = v),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(l10n.cancel),
        ),
        FilledButton(
          onPressed: _selectedId == null
              ? null
              : () => Navigator.of(context).pop(_selectedId),
          child: Text(l10n.save),
        ),
      ],
    );
  }
}

class _DebtInstanceTile extends ConsumerWidget {
  const _DebtInstanceTile({required this.item, this.isLast = false});
  final DebtListItem item;
  final bool isLast;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final debt = item.debt;
    final isPaid = debt.paidCurrentPeriod;
    final cs = Theme.of(context).colorScheme;

    return Dismissible(
      key: Key(debt.id),
      direction: DismissDirection.horizontal,
      confirmDismiss: (direction) async {
        final accessLevels = ref.read(categoryAccessLevelsProvider).valueOrNull;
        final level = accessLevels?[debt.categoryId];

        if (direction == DismissDirection.startToEnd) {
          final hasWrite = PermissionUtils.check(
            context,
            isOwner: item.owned,
            level: level,
            action: (l) => l.canWrite,
          );
          if (!hasWrite) return false;
          await _showPaymentOptions(context, ref, item);
          return false;
        }

        final hasDelete = PermissionUtils.check(
          context,
          isOwner: item.owned,
          level: level,
          action: (l) => l.canDelete,
        );
        if (!hasDelete) return false;

        final confirmed = await showConfirmDialog(
          context,
          title: context.l10n.deleteDebt,
          content: context.l10n.deleteDebtPermanentWarning,
        );
        return confirmed;
      },
      onDismissed: (_) async {
        await ref.read(debtListProvider.notifier).deleteDebt(debt.id);
      },
      background: Container(
        color: Colors.green,
        alignment: Alignment.centerLeft,
        padding: const EdgeInsets.only(left: 20),
        child: const Icon(
          Icons.payments_outlined,
          color: Colors.white,
          size: 26,
        ),
      ),
      secondaryBackground: Container(
        color: const Color(0xFFEF4444),
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 20),
        child: const Icon(Icons.delete_rounded, color: Colors.white, size: 26),
      ),
      child: Container(
        decoration: BoxDecoration(
          border: Border(
            left: BorderSide(
              color: (isPaid ? Colors.green : cs.primary).withValues(
                alpha: 0.45,
              ),
              width: 3,
            ),
          ),
        ),
        child: ListTile(
          onTap: () => context.pushNamed(
            RouteNames.debtDetail,
            pathParameters: {'id': debt.id},
          ),
          contentPadding: const EdgeInsets.fromLTRB(14, 4, 16, 4),
          leading: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: (isPaid ? Colors.green : cs.primary).withValues(
                alpha: 0.1,
              ),
              shape: BoxShape.circle,
            ),
            child: Icon(
              isPaid ? Icons.check_circle_outline : Icons.access_time,
              color: isPaid ? Colors.green : cs.primary,
              size: 20,
            ),
          ),
          title: Row(
            children: [
              Expanded(
                child: Text(
                  debt.name,
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    decoration: null,
                    color: isPaid ? cs.onSurfaceVariant : cs.onSurface,
                  ),
                ),
              ),
              if (debt.hasReminder)
                Padding(
                  padding: const EdgeInsets.only(left: 4),
                  child: Icon(
                    Icons.notifications_active_outlined,
                    size: 14,
                    color: cs.primary.withValues(alpha: 0.7),
                  ),
                ),
            ],
          ),
          subtitle: Text(
            debt.installmentCount != null
                ? '${debt.paidCount} / ${debt.installmentCount}'
                : '${debt.paidCount}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          trailing: Text(
            '${debt.amount} ${debt.currency}',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: isPaid ? cs.onSurfaceVariant : cs.primary,
            ),
          ),
        ),
      ),
    );
  }
}

// ── Payment Options ──────────────────────────────────────────────────────────

Future<void> _showPaymentOptions(
  BuildContext context,
  WidgetRef ref,
  DebtListItem item,
) async {
  final debt = item.debt;
  final l10n = context.l10n;
  final notifier = ref.read(debtListProvider.notifier);

  await showModalBottomSheet<void>(
    context: context,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
    ),
    builder: (ctx) => SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 8),
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Theme.of(ctx).colorScheme.outlineVariant,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Text(
              l10n.paymentActions,
              style: Theme.of(
                ctx,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
          ),
          const Divider(height: 1),
          // ── Ödemeyi Atla ─────────────────────────────────────────────
          ListTile(
            leading: const Icon(Icons.skip_next_outlined, color: Colors.orange),
            title: Text(l10n.skipPayment),
            onTap: () async {
              Navigator.of(ctx).pop();
              try {
                await notifier.skipPayment(debt.id);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(l10n.paymentSkipped(debt.name))),
                  );
                }
              } catch (e) {
                if (context.mounted) showErrorSnackBar(context, e);
              }
            },
          ),
          // ── Parçalı Ödeme ─────────────────────────────────────────────
          ListTile(
            leading: const Icon(Icons.payments_outlined, color: Colors.blue),
            title: Text(l10n.partialPayment),
            onTap: () async {
              Navigator.of(ctx).pop();
              final amount = await showDialog<double>(
                context: context,
                builder: (_) => _AmountDialog(debtName: debt.name),
              );
              if (amount == null) return;
              try {
                await notifier.makePayment(debt.id, amountPaid: amount);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(l10n.paymentRecorded(debt.name))),
                  );
                }
              } catch (e) {
                if (context.mounted) showErrorSnackBar(context, e);
              }
            },
          ),
          // ── Tam Ödeme ─────────────────────────────────────────────────
          ListTile(
            leading: const Icon(
              Icons.check_circle_outline,
              color: Colors.green,
            ),
            title: Text(l10n.fullPayment),
            onTap: () async {
              Navigator.of(ctx).pop();
              try {
                await notifier.makePayment(debt.id);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(l10n.paymentRecorded(debt.name))),
                  );
                }
              } catch (e) {
                if (context.mounted) showErrorSnackBar(context, e);
              }
            },
          ),
          // ── Toplu Ödeme ───────────────────────────────────────────────
          Builder(
            builder: (ctx2) {
              final isPremium = ref.read(isPremiumProvider);
              return ListTile(
                leading: const Icon(
                  Icons.playlist_add_check,
                  color: Colors.purple,
                ),
                title: Text(l10n.bulkPayment),
                trailing: isPremium ? null : const ProBadge(),
                onTap: () async {
                  if (!isPremium) {
                    Navigator.of(ctx).pop();
                    PremiumRequiredSheet.show(
                      context,
                      featureName: l10n.bulkPayment.toLowerCase(),
                      limitCount: 0,
                      message: l10n.premiumFeatureRequired(l10n.bulkPayment),
                    );
                    return;
                  }
                  Navigator.of(ctx).pop();
                  final payments = await showDialog<List<BulkPaymentEntry>>(
                    context: context,
                    builder: (_) => _BulkPayDialog(
                      debtId: debt.id,
                      defaultAmount: debt.amount,
                    ),
                  );
                  if (payments == null || payments.isEmpty) return;
                  try {
                    await notifier.bulkPay(debt.id, payments);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(l10n.bulkPayConfirm(payments.length)),
                        ),
                      );
                    }
                  } catch (e) {
                    if (context.mounted) showErrorSnackBar(context, e);
                  }
                },
              );
            },
          ),
          const SizedBox(height: 8),
        ],
      ),
    ),
  );
}

// ── Amount Dialog ─────────────────────────────────────────────────────────────

class _AmountDialog extends StatefulWidget {
  const _AmountDialog({required this.debtName});
  final String debtName;

  @override
  State<_AmountDialog> createState() => _AmountDialogState();
}

class _AmountDialogState extends State<_AmountDialog> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AlertDialog(
      title: Text(l10n.enterAmount),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.debtName,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _controller,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              hintText: l10n.partialPaymentHint,
              border: const OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(MaterialLocalizations.of(context).cancelButtonLabel),
        ),
        FilledButton(
          onPressed: () {
            final value = double.tryParse(
              _controller.text.replaceAll(',', '.'),
            );
            Navigator.of(context).pop(value);
          },
          child: Text(MaterialLocalizations.of(context).okButtonLabel),
        ),
      ],
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
                final label = DateFormat(
                  'MMM',
                  locale,
                ).format(DateTime(_year, month));

                return InkWell(
                  onTap: () =>
                      Navigator.of(context).pop(DateTime(_year, month)),
                  borderRadius: BorderRadius.circular(10),
                  child: Container(
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color:
                          isCurrentSelection &&
                              _year == widget.initialMonth.year
                          ? cs.primary
                          : cs.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      label,
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color:
                            isCurrentSelection &&
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

// ── Bulk Pay Dialog ──────────────────────────────────────────────────────────

class _BulkPayDialog extends ConsumerStatefulWidget {
  const _BulkPayDialog({required this.debtId, required this.defaultAmount});
  final String debtId;
  final double defaultAmount;

  @override
  ConsumerState<_BulkPayDialog> createState() => _BulkPayDialogState();
}

class _BulkPayDialogState extends ConsumerState<_BulkPayDialog> {
  final List<_BulkEntry> _entries = [];
  bool _initialized = false;

  void _initEntries(Debt debt) {
    if (_initialized) return;
    _initialized = true;
    final amountStr = debt.amount % 1 == 0
        ? debt.amount.toInt().toString()
        : debt.amount.toString();
    final now = DateTime.now();
    final maxCount = debt.installmentCount;
    int index = 0;
    while (true) {
      if (maxCount != null && index >= maxCount) break;
      if (index >= 1000) break;
      final date = _installmentDate(
        debt.firstPaymentDate,
        debt.paymentPeriodType,
        index,
      );
      if (date.isAfter(now)) break;
      final alreadyPaid =
          index < debt.payments.length && debt.payments[index].paid;
      if (!alreadyPaid) {
        _entries.add(
          _BulkEntry(
            date: date,
            controller: TextEditingController(text: amountStr),
            selected: true,
          ),
        );
      }
      index++;
    }
  }

  @override
  void dispose() {
    for (final e in _entries) {
      e.controller.dispose();
    }
    super.dispose();
  }

  static DateTime _installmentDate(
    DateTime start,
    PaymentPeriodType period,
    int index,
  ) => switch (period) {
    PaymentPeriodType.oneTime => start,
    PaymentPeriodType.daily => start.add(Duration(days: index)),
    PaymentPeriodType.weekly => start.add(Duration(days: index * 7)),
    PaymentPeriodType.biWeekly => start.add(Duration(days: index * 14)),
    PaymentPeriodType.monthly => DateTime(
      start.year,
      start.month + index,
      start.day,
    ),
    PaymentPeriodType.quarterly => DateTime(
      start.year,
      start.month + index * 3,
      start.day,
    ),
    PaymentPeriodType.halfYearly => DateTime(
      start.year,
      start.month + index * 6,
      start.day,
    ),
    PaymentPeriodType.yearly => DateTime(
      start.year + index,
      start.month,
      start.day,
    ),
  };

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final debtAsync = ref.watch(debtDetailProvider(widget.debtId));

    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: debtAsync.when(
        loading: () => const Padding(
          padding: EdgeInsets.all(48),
          child: Center(child: CircularProgressIndicator()),
        ),
        error: (e, _) => Padding(
          padding: const EdgeInsets.all(20),
          child: Text(e.toString()),
        ),
        data: (debt) {
          _initEntries(debt);
          final selectedCount = _entries.where((e) => e.selected).length;

          return Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.bulkPayTitle,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 12),
                if (_entries.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 20),
                    child: Center(child: Text(l10n.noPaymentsYet)),
                  )
                else
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 360),
                    child: SingleChildScrollView(
                      child: Column(
                        children: List.generate(_entries.length, (i) {
                          final entry = _entries[i];
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 4),
                            child: Row(
                              children: [
                                Checkbox(
                                  value: entry.selected,
                                  onChanged: (val) => setState(
                                    () => entry.selected = val ?? false,
                                  ),
                                ),
                                const SizedBox(width: 4),
                                Expanded(
                                  child: Text(
                                    DateFormatter.formatDate(entry.date),
                                    style: Theme.of(
                                      context,
                                    ).textTheme.bodyMedium,
                                  ),
                                ),
                                SizedBox(
                                  width: 90,
                                  child: TextField(
                                    controller: entry.controller,
                                    keyboardType:
                                        const TextInputType.numberWithOptions(
                                          decimal: true,
                                        ),
                                    decoration: InputDecoration(
                                      isDense: true,
                                      contentPadding:
                                          const EdgeInsets.symmetric(
                                            horizontal: 8,
                                            vertical: 6,
                                          ),
                                      border: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                    ),
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyMedium
                                        ?.copyWith(fontWeight: FontWeight.w600),
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                      ),
                    ),
                  ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    TextButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: Text(
                        MaterialLocalizations.of(context).cancelButtonLabel,
                      ),
                    ),
                    const SizedBox(width: 8),
                    FilledButton.icon(
                      onPressed: selectedCount == 0
                          ? null
                          : () {
                              final result = _entries
                                  .where((e) => e.selected)
                                  .map(
                                    (e) => BulkPaymentEntry(
                                      amountPaid:
                                          double.tryParse(
                                            e.controller.text.replaceAll(
                                              ',',
                                              '.',
                                            ),
                                          ) ??
                                          widget.defaultAmount,
                                      paidTime: e.date,
                                    ),
                                  )
                                  .toList();
                              Navigator.of(context).pop(result);
                            },
                      icon: const Icon(Icons.check, size: 18),
                      label: Text('${l10n.send} ($selectedCount)'),
                    ),
                  ],
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _BulkEntry {
  _BulkEntry({
    required this.date,
    required this.controller,
    this.selected = false,
  });
  final DateTime date;
  final TextEditingController controller;
  bool selected;
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
