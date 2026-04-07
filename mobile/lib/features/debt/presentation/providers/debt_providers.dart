import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../../data/datasources/debt_remote_datasource.dart';
import '../../data/models/currency_model.dart';
import '../../data/models/debt_model.dart';
import '../../data/repositories/debt_repository_impl.dart';
import '../../domain/entities/category_with_debts.dart';
import '../../domain/entities/debt.dart';

part 'debt_providers.g.dart';

@riverpod
class DebtList extends _$DebtList {
  @override
  Future<List<Debt>> build() async {
    final month = ref.watch(selectedMonthProvider);
    final monthStr =
        '${month.year}-${month.month.toString().padLeft(2, '0')}';
    return ref.watch(debtRepositoryProvider).getAll(monthStr);
  }

  Future<void> deleteDebt(String id) async {
    await ref.read(debtRepositoryProvider).delete(id);
    ref.invalidateSelf();
  }

  Future<void> makePayment(String debtId) async {
    await ref.read(debtRepositoryProvider).pay(debtId);
    ref.invalidateSelf();
    ref.invalidate(debtGroupedProvider);
    ref.invalidate(debtDetailProvider(debtId));
  }

  Future<void> skipPayment(String debtId) async {
    await ref.read(debtRepositoryProvider).skip(debtId);
    ref.invalidateSelf();
    ref.invalidate(debtGroupedProvider);
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
class DebtGrouped extends _$DebtGrouped {
  @override
  Future<List<CategoryWithDebts>> build() async {
    final raw = await ref.watch(debtRemoteDatasourceProvider).getGrouped();
    final grouped = raw
        .map(
          (r) => CategoryWithDebts(
            categoryId: r.categoryId,
            categoryName: r.categoryName,
            owned: r.owned,
            debts: r.debts.map((d) => d.toDomain()).toList(),
          ),
        )
        .toList();

    final allCategories = await ref.watch(categoryListProvider.future);
    final groupedIds = grouped.map((g) => g.categoryId).toSet();
    final currentUserId = await ref
        .read(secureStorageServiceProvider)
        .getUserId();

    final emptyGroups = allCategories
        .where((c) => !groupedIds.contains(c.id))
        .map(
          (c) => CategoryWithDebts(
            categoryId: c.id,
            categoryName: c.name,
            owned: c.ownerId == currentUserId,
            debts: const [],
          ),
        )
        .toList();

    return [...grouped, ...emptyGroups];
  }

  Future<void> refresh() async {
    ref.invalidateSelf();
    await future;
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

class DebtPaymentInstance {
  DebtPaymentInstance({
    required this.debt,
    required this.paymentDate,
    required this.installmentIndex,
    this.isPaid = false,
  });
  final Debt debt;
  final DateTime paymentDate;
  final int installmentIndex;
  final bool isPaid;
}

@riverpod
Future<List<DebtPaymentInstance>> debtPaymentInstances(
  DebtPaymentInstancesRef ref,
) async {
  final debts = await ref.watch(debtListProvider.future);
  final month = ref.watch(selectedMonthProvider);
  final startOfMonth = DateTime(month.year, month.month, 1);
  final endOfMonth = DateTime(month.year, month.month + 1, 0, 23, 59, 59);

  final List<DebtPaymentInstance> instances = [];

  for (final debt in debts) {
    DateTime current = debt.firstPaymentDate;
    int index = 1;

    while (true) {
      if (debt.installmentCount != null && index > debt.installmentCount!) {
        break;
      }
      if (current.isAfter(endOfMonth)) break;

      if (current.isAfter(startOfMonth.subtract(const Duration(seconds: 1))) &&
          current.isBefore(endOfMonth.add(const Duration(seconds: 1)))) {
        final isPaid = debt.paymentStatus == PaymentStatus.paid;

        instances.add(
          DebtPaymentInstance(
            debt: debt,
            paymentDate: current,
            installmentIndex: index,
            isPaid: isPaid,
          ),
        );
      }

      // Advance current based on period
      current = _nextDate(current, debt.paymentPeriodType);
      index++;

      // Safety break to prevent infinite loops if something goes wrong
      if (index > 1000) break;
    }
  }

  return instances;
}

DateTime _nextDate(DateTime current, PaymentPeriodType period) {
  switch (period) {
    case PaymentPeriodType.daily:
      return current.add(const Duration(days: 1));
    case PaymentPeriodType.weekly:
      return current.add(const Duration(days: 7));
    case PaymentPeriodType.biWeekly:
      return current.add(const Duration(days: 14));
    case PaymentPeriodType.monthly:
      return DateTime(current.year, current.month + 1, current.day);
    case PaymentPeriodType.quarterly:
      return DateTime(current.year, current.month + 3, current.day);
    case PaymentPeriodType.halfYearly:
      return DateTime(current.year, current.month + 6, current.day);
    case PaymentPeriodType.yearly:
      return DateTime(current.year + 1, current.month, current.day);
  }
}

typedef GroupedDebtsByPeriod =
    Map<
      PaymentPeriodType,
      Map<
        String,
        ({String categoryName, bool owned, List<DebtPaymentInstance> instances})
      >
    >;

@riverpod
Future<GroupedDebtsByPeriod> groupedDebtInstances(
  GroupedDebtInstancesRef ref,
) async {
  final instances = await ref.watch(debtPaymentInstancesProvider.future);
  final allCategories = await ref.watch(categoryListProvider.future);
  final currentUserId = await ref
      .read(secureStorageServiceProvider)
      .getUserId();

  // Initialize map for all periods
  final GroupedDebtsByPeriod grouped = {
    for (final period in PaymentPeriodType.values) period: {},
  };

  for (final instance in instances) {
    final period = instance.debt.paymentPeriodType;
    final catId = instance.debt.categoryId ?? 'no_category';

    if (!grouped[period]!.containsKey(catId)) {
      final category = allCategories.where((c) => c.id == catId).firstOrNull;
      grouped[period]![catId] = (
        categoryName: category?.name ?? 'Kategori Yok',
        owned: category?.ownerId == currentUserId,
        instances: [],
      );
    }

    grouped[period]![catId]!.instances.add(instance);
  }

  return grouped;
}

@riverpod
Future<List<CurrencyModel>> currencyList(Ref ref) async {
  final data = await rootBundle.loadString('assets/currencies.json');
  final list = jsonDecode(data) as List<dynamic>;
  return list
      .map((e) => CurrencyModel.fromJson(e as Map<String, dynamic>))
      .toList();
}
