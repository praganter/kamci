import 'package:kamci/features/charts/data/models/monthly_summary_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('MonthlySummaryModel.baseCurrency', () {
    test('debt/summary baseCurrency alanı domain modele taşınır', () {
      final summary = MonthlySummaryModel.fromJson({
        'month': '2026-10',
        'baseCurrency': 'USD',
        'totalPaid': 1500,
      }).toDomain();

      expect(summary.baseCurrency, 'USD');
      expect(summary.totalPaid, 1500.0);
    });

    test('alan yoksa (eski backend) null kalır', () {
      final summary = MonthlySummaryModel.fromJson({
        'month': '2026-10',
      }).toDomain();

      expect(summary.baseCurrency, isNull);
    });
  });
}
