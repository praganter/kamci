import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../features/debt/data/datasources/debt_remote_datasource.dart';
import '../../../../features/debt/presentation/providers/debt_providers.dart';
import '../../../charts/data/models/monthly_summary_model.dart';
import '../../../charts/domain/entities/monthly_summary.dart';

final monthlySummaryProvider =
    FutureProvider.autoDispose<MonthlySummary>((ref) async {
  final month = ref.watch(selectedMonthProvider);
  final monthStr =
      '${month.year}-${month.month.toString().padLeft(2, '0')}';
  final raw =
      await ref.read(debtRemoteDatasourceProvider).getSummary(monthStr);
  return MonthlySummaryModel.fromJson(raw).toDomain();
});
