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
    return ref.watch(debtRepositoryProvider).getAll();
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
    final grouped = raw.map((r) => CategoryWithDebts(
      categoryId: r.categoryId,
      categoryName: r.categoryName,
      owned: r.owned,
      debts: r.debts.map((d) => d.toDomain()).toList(),
    )).toList();

    final allCategories = await ref.watch(categoryListProvider.future);
    final groupedIds = grouped.map((g) => g.categoryId).toSet();
    final currentUserId = await ref.read(secureStorageServiceProvider).getUserId();

    final emptyGroups = allCategories
        .where((c) => !groupedIds.contains(c.id))
        .map((c) => CategoryWithDebts(
              categoryId: c.id,
              categoryName: c.name,
              owned: c.ownerId == currentUserId,
              debts: const [],
            ))
        .toList();

    return [...grouped, ...emptyGroups];
  }

  Future<void> refresh() async {
    ref.invalidateSelf();
    await future;
  }
}

@riverpod
Future<List<CurrencyModel>> currencyList(Ref ref) async {
  final data = await rootBundle.loadString('assets/currencies.json');
  final list = jsonDecode(data) as List<dynamic>;
  return list.map((e) => CurrencyModel.fromJson(e as Map<String, dynamic>)).toList();
}

