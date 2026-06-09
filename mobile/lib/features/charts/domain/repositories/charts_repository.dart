import '../entities/monthly_summary.dart';

abstract interface class ChartsRepository {
  Future<MonthlySummary> getMonthlySummary(String month);
}
