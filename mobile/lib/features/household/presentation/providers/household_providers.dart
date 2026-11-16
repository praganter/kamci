import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../data/repositories/household_repository_impl.dart';
import '../../domain/entities/household.dart';

part 'household_providers.g.dart';

@riverpod
class HouseholdCurrent extends _$HouseholdCurrent {
  @override
  Future<Household?> build() =>
      ref.watch(householdRepositoryProvider).getCurrent();

  Future<void> invite(String email) async {
    await ref.read(householdRepositoryProvider).invite(email);
    ref.invalidateSelf();
  }

  Future<void> accept(String householdId) async {
    await ref.read(householdRepositoryProvider).accept(householdId);
    ref.invalidateSelf();
  }

  Future<void> decline(String householdId) async {
    await ref.read(householdRepositoryProvider).decline(householdId);
    ref.invalidateSelf();
  }

  Future<void> leave() async {
    await ref.read(householdRepositoryProvider).leave();
    ref.invalidateSelf();
  }
}
