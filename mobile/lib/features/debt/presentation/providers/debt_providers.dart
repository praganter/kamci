import 'package:riverpod_annotation/riverpod_annotation.dart';
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
}

@riverpod
class DebtDetail extends _$DebtDetail {
  @override
  Future<Debt> build(String id) async {
    final debts = await ref.watch(debtListProvider.future);
    return debts.firstWhere(
      (d) => d.id == id,
      orElse: () => throw Exception('Borç bulunamadı'),
    );
  }
}

@riverpod
class DebtGrouped extends _$DebtGrouped {
  @override
  Future<List<CategoryWithDebts>> build() async {
    final raw = await ref.watch(debtRemoteDatasourceProvider).getGrouped();
    return raw.map((r) => CategoryWithDebts(
      categoryId: r.categoryId,
      categoryName: r.categoryName,
      owned: r.owned,
      debts: r.debts.map((d) => d.toDomain()).toList(),
    )).toList();
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
  DateTime? startDate,
  DateTime? endDate,
  PaymentPeriodType? paymentPeriodType,
  String amount,
  DebtStatus status,
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
    startDate: null,
    endDate: null,
    paymentPeriodType: PaymentPeriodType.monthly,
    amount: '',
    status: DebtStatus.active,
    isLoading: false,
    error: null,
    isSuccess: false,
  );

  void updateName(String v) => state = (
    name: v,
    categoryId: state.categoryId,
    description: state.description,
    startDate: state.startDate,
    endDate: state.endDate,
    paymentPeriodType: state.paymentPeriodType,
    amount: state.amount,
    status: state.status,
    isLoading: state.isLoading,
    error: null,
    isSuccess: false,
  );

  void updateField({
    String? name,
    String? categoryId,
    String? description,
    DateTime? startDate,
    DateTime? endDate,
    PaymentPeriodType? paymentPeriodType,
    String? amount,
    DebtStatus? status,
  }) {
    state = (
      name: name ?? state.name,
      categoryId: categoryId ?? state.categoryId,
      description: description ?? state.description,
      startDate: startDate ?? state.startDate,
      endDate: endDate ?? state.endDate,
      paymentPeriodType: paymentPeriodType ?? state.paymentPeriodType,
      amount: amount ?? state.amount,
      status: status ?? state.status,
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
      startDate: state.startDate,
      endDate: state.endDate,
      paymentPeriodType: state.paymentPeriodType,
      amount: state.amount,
      status: state.status,
      isLoading: true,
      error: null,
      isSuccess: false,
    );

    try {
      final params = CreateDebtParams(
        name: state.name,
        categoryId: state.categoryId,
        description: state.description,
        startDate: state.startDate!,
        endDate: state.endDate!,
        paymentPeriodType: state.paymentPeriodType!,
        amount: double.parse(state.amount.replaceAll(',', '.')),
        status: state.status,
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
        startDate: state.startDate,
        endDate: state.endDate,
        paymentPeriodType: state.paymentPeriodType,
        amount: state.amount,
        status: state.status,
        isLoading: false,
        error: null,
        isSuccess: true,
      );
    } catch (e) {
      state = (
        name: state.name,
        categoryId: state.categoryId,
        description: state.description,
        startDate: state.startDate,
        endDate: state.endDate,
        paymentPeriodType: state.paymentPeriodType,
        amount: state.amount,
        status: state.status,
        isLoading: false,
        error: e.toString(),
        isSuccess: false,
      );
    }
  }
}
