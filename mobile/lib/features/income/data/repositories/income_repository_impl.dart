import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/income.dart';
import '../../domain/repositories/income_repository.dart';
import '../datasources/income_remote_datasource.dart';
import '../models/income_model.dart';
import '../models/save_income_request.dart';

part 'income_repository_impl.g.dart';

@riverpod
IncomeRepository incomeRepository(IncomeRepositoryRef ref) =>
    IncomeRepositoryImpl(ref.watch(incomeRemoteDatasourceProvider));

class IncomeRepositoryImpl implements IncomeRepository {
  const IncomeRepositoryImpl(this._remote);
  final IncomeRemoteDatasource _remote;

  @override
  Future<Income> save(SaveIncomeRequest request) async {
    final model = await _remote.save(request);
    return model.toDomain();
  }

  @override
  Future<List<Income>> getHistory() async {
    final list = await _remote.getHistory();
    return list.map((e) => e.toDomain()).toList();
  }

  @override
  Future<Income> getCurrent() async {
    final model = await _remote.getCurrent();
    return model.toDomain();
  }

  @override
  Future<void> delete(String id) async {
    await _remote.delete(id);
  }
}
