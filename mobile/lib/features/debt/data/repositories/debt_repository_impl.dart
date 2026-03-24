import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../domain/entities/debt.dart';
import '../../domain/repositories/debt_repository.dart';
import '../datasources/debt_remote_datasource.dart';
import '../models/create_debt_request.dart';
import '../models/debt_model.dart';

part 'debt_repository_impl.g.dart';

@riverpod
DebtRepository debtRepository(Ref ref) =>
    DebtRepositoryImpl(ref.watch(debtRemoteDatasourceProvider));

class DebtRepositoryImpl implements DebtRepository {
  const DebtRepositoryImpl(this._datasource);
  final DebtRemoteDatasource _datasource;

  @override
  Future<List<Debt>> getAll() async {
    final models = await _datasource.getAll();
    return models.map((m) => m.toDomain()).toList();
  }

  @override
  Future<Debt> getById(String id) async {
    final model = await _datasource.getById(id);
    return model.toDomain();
  }

  @override
  Future<Debt> create(CreateDebtParams params) async {
    final request = _paramsToRequest(params);
    final model = await _datasource.create(request);
    return model.toDomain();
  }

  @override
  Future<Debt> update(String id, CreateDebtParams params) async {
    final request = _paramsToRequest(params, id: id);
    final model = await _datasource.update(request);
    return model.toDomain();
  }

  @override
  Future<Debt> pay(String id) async {
    final model = await _datasource.pay(id);
    return model.toDomain();
  }

  @override
  Future<void> delete(String id) => _datasource.delete(id);

  CreateDebtRequest _paramsToRequest(CreateDebtParams params, {String? id}) {
    return CreateDebtRequest(
      id: id ?? params.id,
      name: params.name,
      categoryId: params.categoryId,
      description: params.description,
      firstPaymentDate: DateFormatter.toApiDateTimeFormat(params.firstPaymentDate),
      lastPaymentDate: params.lastPaymentDate != null
          ? DateFormatter.toApiDateTimeFormat(params.lastPaymentDate!)
          : null,
      paymentPeriodType: PaymentPeriodTypeJson.fromDomain(params.paymentPeriodType),
      amount: params.amount,
      autoPayment: params.autoPayment,
    );
  }
}
