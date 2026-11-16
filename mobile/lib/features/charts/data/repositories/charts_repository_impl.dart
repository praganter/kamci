import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../features/debt/data/datasources/debt_remote_datasource.dart';
import '../../data/models/monthly_summary_model.dart';
import '../../domain/entities/monthly_summary.dart';
import '../../domain/repositories/charts_repository.dart';

final chartsRepositoryProvider = Provider<ChartsRepository>(
  (ref) => ChartsRepositoryImpl(ref.watch(debtRemoteDatasourceProvider)),
);

class ChartsRepositoryImpl implements ChartsRepository {
  const ChartsRepositoryImpl(this._datasource);
  final DebtRemoteDatasource _datasource;

  @override
  Future<MonthlySummary> getMonthlySummary(String month) async {
    final raw = await _datasource.getSummary(month);
    return MonthlySummaryModel.fromJson(raw).toDomain();
  }
}
