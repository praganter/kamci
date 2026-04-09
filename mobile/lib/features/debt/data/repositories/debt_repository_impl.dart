import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../domain/entities/debt.dart';
import '../../domain/entities/debt_summary.dart';
import '../../domain/repositories/debt_repository.dart';
import '../datasources/debt_remote_datasource.dart';
import '../models/create_debt_request.dart';
import '../models/debt_model.dart';
import '../models/debt_summary_model.dart';
import '../models/reminder_embedded_model.dart';

part 'debt_repository_impl.g.dart';

@riverpod
DebtRepository debtRepository(Ref ref) =>
    DebtRepositoryImpl(ref.watch(debtRemoteDatasourceProvider));

class DebtRepositoryImpl implements DebtRepository {
  const DebtRepositoryImpl(this._datasource);
  final DebtRemoteDatasource _datasource;

  @override
  Future<List<DebtSummary>> getAll(String month, bool isOwner) async {
    final categories = await _datasource.getGrouped(
      month: month,
      isOwner: isOwner,
    );
    return [
      for (final cat in categories)
        for (final model in cat.debts) model.toDomain(),
    ];
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
  Future<Debt> pay(String id, {double? amountPaid}) async {
    final model = await _datasource.pay(id, amountPaid: amountPaid);
    return model.toDomain();
  }

  @override
  Future<Debt> skip(String id) async {
    final model = await _datasource.skip(id);
    return model.toDomain();
  }

  @override
  Future<Debt> bulkPay(String id, List<Map<String, dynamic>> payments) async {
    final model = await _datasource.bulkPay(id, payments);
    return model.toDomain();
  }

  @override
  Future<void> delete(String id) => _datasource.delete(id);

  @override
  Future<Map<String, dynamic>> getSummary(String month) =>
      _datasource.getSummary(month);

  @override
  Future<List<Map<String, dynamic>>> getPaymentHistory(
    String id,
    DateTime from,
    DateTime to,
  ) => _datasource.getPaymentHistory(id, from, to);

  CreateDebtRequest _paramsToRequest(CreateDebtParams params, {String? id}) {
    return CreateDebtRequest(
      id: id ?? params.id,
      name: params.name,
      categoryId: params.categoryId,
      description: params.description,
      firstPaymentDate: DateFormatter.toApiDateTimeFormat(
        params.firstPaymentDate,
      ),
      installmentCount: params.installmentCount,
      paymentPeriodType: PaymentPeriodTypeJson.fromDomain(
        params.paymentPeriodType,
      ),
      amount: params.amount,
      autoPayment: params.autoPayment,
      currency: params.currency,
      reminder: params.reminder?.toModel(),
    );
  }
}
