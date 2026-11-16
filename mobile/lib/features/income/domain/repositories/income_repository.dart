import '../../domain/entities/income.dart';
import '../../data/models/save_income_request.dart';

abstract class IncomeRepository {
  Future<Income> save(SaveIncomeRequest request);
  Future<List<Income>> getHistory();
  Future<Income> getCurrent();
  Future<void> delete(String id);
}
