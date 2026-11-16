import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/income.dart';
import '../../data/repositories/income_repository_impl.dart';

part 'income_providers.g.dart';

@riverpod
Future<List<Income>> incomeHistory(IncomeHistoryRef ref) {
  return ref.watch(incomeRepositoryProvider).getHistory();
}

@riverpod
Future<Income?> currentIncome(CurrentIncomeRef ref) async {
  try {
    return await ref.watch(incomeRepositoryProvider).getCurrent();
  } catch (_) {
    return null;
  }
}

final removedIncomeIdsProvider = StateProvider<Set<String>>((_) => {});
