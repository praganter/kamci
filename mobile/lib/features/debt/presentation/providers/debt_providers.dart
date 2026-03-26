import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../../data/datasources/debt_remote_datasource.dart';
import '../../data/models/debt_model.dart';
import '../../data/repositories/debt_repository_impl.dart';
import '../../domain/entities/category_with_debts.dart';
import '../../domain/entities/debt.dart';
import '../../domain/repositories/debt_repository.dart';

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

typedef DebtFormState = ({
  String name,
  String? categoryId,
  String? description,
  DateTime? firstPaymentDate,
  DateTime? lastPaymentDate,
  PaymentPeriodType? paymentPeriodType,
  String amount,
  bool isLoading,
  String? error,
  bool isSuccess,
});

@riverpod
class DebtForm extends _$DebtForm {
  @override
  DebtFormState build() => (
    name: '',
    categoryId: null,
    description: null,
    firstPaymentDate: null,
    lastPaymentDate: null,
    paymentPeriodType: PaymentPeriodType.monthly,
    amount: '',
    isLoading: false,
    error: null,
    isSuccess: false,
  );

  void updateName(String v) => state = (
    name: v,
    categoryId: state.categoryId,
    description: state.description,
    firstPaymentDate: state.firstPaymentDate,
    lastPaymentDate: state.lastPaymentDate,
    paymentPeriodType: state.paymentPeriodType,
    amount: state.amount,
    isLoading: state.isLoading,
    error: null,
    isSuccess: false,
  );

  void updateField({
    String? name,
    String? categoryId,
    String? description,
    DateTime? firstPaymentDate,
    DateTime? lastPaymentDate,
    PaymentPeriodType? paymentPeriodType,
    String? amount,
  }) {
    state = (
      name: name ?? state.name,
      categoryId: categoryId ?? state.categoryId,
      description: description ?? state.description,
      firstPaymentDate: firstPaymentDate ?? state.firstPaymentDate,
      lastPaymentDate: lastPaymentDate ?? state.lastPaymentDate,
      paymentPeriodType: paymentPeriodType ?? state.paymentPeriodType,
      amount: amount ?? state.amount,
      isLoading: false,
      error: null,
      isSuccess: false,
    );
  }

  Future<void> submit({String? debtId}) async {
    state = (
      name: state.name,
      categoryId: state.categoryId,
      description: state.description,
      firstPaymentDate: state.firstPaymentDate,
      lastPaymentDate: state.lastPaymentDate,
      paymentPeriodType: state.paymentPeriodType,
      amount: state.amount,
      isLoading: true,
      error: null,
      isSuccess: false,
    );

    try {
      final params = CreateDebtParams(
        name: state.name,
        categoryId: state.categoryId,
        description: state.description,
        firstPaymentDate: state.firstPaymentDate!,
        lastPaymentDate: state.lastPaymentDate,
        paymentPeriodType: state.paymentPeriodType!,
        amount: double.parse(state.amount.replaceAll(',', '.')),
      );

      final repo = ref.read(debtRepositoryProvider);
      if (debtId != null) {
        await repo.update(debtId, params);
      } else {
        await repo.create(params);
      }

      ref.invalidate(debtListProvider);
      state = (
        name: state.name,
        categoryId: state.categoryId,
        description: state.description,
        firstPaymentDate: state.firstPaymentDate,
        lastPaymentDate: state.lastPaymentDate,
        paymentPeriodType: state.paymentPeriodType,
        amount: state.amount,
        isLoading: false,
        error: null,
        isSuccess: true,
      );
    } catch (e) {
      state = (
        name: state.name,
        categoryId: state.categoryId,
        description: state.description,
        firstPaymentDate: state.firstPaymentDate,
        lastPaymentDate: state.lastPaymentDate,
        paymentPeriodType: state.paymentPeriodType,
        amount: state.amount,
        isLoading: false,
        error: e.toString(),
        isSuccess: false,
      );
    }
  }
}
