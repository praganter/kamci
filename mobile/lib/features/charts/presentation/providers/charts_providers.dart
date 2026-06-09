import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../features/debt/presentation/providers/debt_providers.dart';
import '../../../charts/data/repositories/charts_repository_impl.dart';
import '../../../charts/domain/entities/monthly_summary.dart';

final monthlySummaryProvider =
    FutureProvider.autoDispose<MonthlySummary>((ref) async {
  final month = ref.watch(selectedMonthProvider);
  final monthStr =
      '${month.year}-${month.month.toString().padLeft(2, '0')}';
  return ref.watch(chartsRepositoryProvider).getMonthlySummary(monthStr);
});
