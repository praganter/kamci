import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/providers/app_settings_provider.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../../domain/entities/bulk_payment_entry.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../data/models/currency_model.dart';
import '../../data/repositories/debt_repository_impl.dart';
import '../../domain/entities/category_with_debts.dart';
import '../../domain/entities/debt.dart';
import '../../domain/entities/debt_summary.dart';
import '../../domain/repositories/debt_repository.dart';

part 'debt_providers.g.dart';


@riverpod
Future<List<CategoryWithDebts>> debtGrouped(Ref ref, bool isOwner) async {
  final month = ref.watch(selectedMonthProvider);
  final monthStr = '${month.year}-${month.month.toString().padLeft(2, '0')}';

  final grouped = await ref
      .watch(debtRepositoryProvider)
      .getGrouped(monthStr, isOwner);

  if (isOwner) {
    final allCategories = await ref.watch(categoryListProvider.future);
    final currentUserId = await ref.watch(currentUserIdProvider.future);
    final groupedIds = grouped.map((g) => g.categoryId).toSet();
    final emptyGroups = allCategories
        .where((c) => c.ownerId == currentUserId && !groupedIds.contains(c.id))
        .map(
          (c) => CategoryWithDebts(
            categoryId: c.id,
            categoryName: c.name,
            owned: true,
            debts: const [],
          ),
        )
        .toList();
    return [...grouped, ...emptyGroups];
  }

  return grouped;
}


@riverpod
class DebtList extends _$DebtList {
  @override
  Future<List<DebtSummary>> build() async {
    final tab = ref.watch(debtTabProvider);
    final isOwner = tab == 0;
    final categories = await ref.watch(debtGroupedProvider(isOwner).future);
    return [for (final cat in categories) ...cat.debts];
  }

  void _invalidateAll() {
    ref.invalidate(debtGroupedProvider(true));
    ref.invalidate(debtGroupedProvider(false));
  }

  Future<void> saveDebt(String? debtId, CreateDebtParams params) async {
    if (debtId != null) {
      await ref.read(debtRepositoryProvider).update(debtId, params);
      ref.invalidate(debtDetailProvider(debtId));
    } else {
      await ref.read(debtRepositoryProvider).create(params);
    }
  }

  Future<void> deleteDebt(String id) async {
    await ref.read(debtRepositoryProvider).delete(id);
    _invalidateAll();
  }

  Future<void> makePayment(String debtId, {double? amountPaid}) async {
    await ref.read(debtRepositoryProvider).pay(debtId, amountPaid: amountPaid);
    _invalidateAll();
    ref.invalidate(debtDetailProvider(debtId));
  }

  Future<void> skipPayment(String debtId) async {
    await ref.read(debtRepositoryProvider).skip(debtId);
    _invalidateAll();
    ref.invalidate(debtDetailProvider(debtId));
  }

  Future<void> setPaidInstallments(String debtId, int count) async {
    await ref.read(debtRepositoryProvider).setPaidInstallments(debtId, count);
    _invalidateAll();
    ref.invalidate(debtDetailProvider(debtId));
  }

  Future<void> bulkPay(String debtId, List<BulkPaymentEntry> payments) async {
    await ref.read(debtRepositoryProvider).bulkPay(debtId, payments);
    _invalidateAll();
    ref.invalidate(debtDetailProvider(debtId));
  }
}


@riverpod
class DebtDetail extends _$DebtDetail {
  @override
  Future<Debt> build(String id) {
    ref.keepAlive();
    return ref.read(debtRepositoryProvider).getById(id);
  }
}


@riverpod
class SelectedMonth extends _$SelectedMonth {
  @override
  DateTime build() {
    final now = DateTime.now();
    return DateTime(now.year, now.month);
  }

  void setMonth(DateTime month) => state = DateTime(month.year, month.month);
  void next() => state = DateTime(state.year, state.month + 1);
  void previous() => state = DateTime(state.year, state.month - 1);
}


class DebtListItem {
  const DebtListItem({
    required this.debt,
    required this.categoryName,
    required this.owned,
    this.ownerUserId,
  });

  final DebtSummary debt;
  final String categoryName;
  final bool owned;
  final String? ownerUserId;
}


typedef GroupedDebtsByPeriod =
    Map<
      PaymentPeriodType,
      Map<
        String,
        ({
          String categoryName,
          bool owned,
          String? ownerUserId,
          List<DebtListItem> items,
        })
      >
    >;

GroupedDebtsByPeriod _buildGrouped(List<CategoryWithDebts> categories) {
  final GroupedDebtsByPeriod grouped = {
    for (final period in PaymentPeriodType.values) period: {},
  };

  for (final cat in categories) {
    for (final debt in cat.debts) {
      final period = debt.paymentPeriodType;
      final catId = cat.categoryId;

      if (!grouped[period]!.containsKey(catId)) {
        grouped[period]![catId] = (
          categoryName: cat.categoryName,
          owned: cat.owned,
          ownerUserId: cat.ownerUserId,
          items: [],
        );
      }

      grouped[period]![catId]!.items.add(
        DebtListItem(
          debt: debt,
          categoryName: cat.categoryName,
          owned: cat.owned,
          ownerUserId: cat.ownerUserId,
        ),
      );
    }
  }

  return grouped;
}


@riverpod
Future<GroupedDebtsByPeriod> groupedDebtInstancesByOwner(
  Ref ref,
  bool isOwner,
) async {
  final categories = await ref.watch(debtGroupedProvider(isOwner).future);
  return _buildGrouped(categories);
}


@Riverpod(keepAlive: true)
Future<List<CurrencyModel>> currencyList(Ref ref) async {
  final data = await rootBundle.loadString('assets/currencies.json');
  final list = jsonDecode(data) as List<dynamic>;
  return list
      .map((e) => CurrencyModel.fromJson(e as Map<String, dynamic>))
      .toList();
}


@riverpod
Future<Map<String, double>> exchangeRates(Ref ref) async {
  final base = ref.watch(baseCurrencyProvider);
  final dio = ref.watch(dioClientProvider);
  final resp = await dio.get('${ApiConstants.exchangeRates}/$base');
  final body = resp.data as Map<String, dynamic>;
  final rates = (body['rates'] ?? body) as Map<String, dynamic>;
  return {for (final e in rates.entries) e.key: (e.value as num).toDouble()};
}
