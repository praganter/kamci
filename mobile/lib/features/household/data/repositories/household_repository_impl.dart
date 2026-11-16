import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/household.dart';
import '../../domain/repositories/household_repository.dart';
import '../datasources/household_remote_datasource.dart';
import '../models/household_model.dart';

part 'household_repository_impl.g.dart';

@riverpod
HouseholdRepository householdRepository(Ref ref) =>
    HouseholdRepositoryImpl(ref.watch(householdRemoteDatasourceProvider));

class HouseholdRepositoryImpl implements HouseholdRepository {
  const HouseholdRepositoryImpl(this._datasource);
  final HouseholdRemoteDatasource _datasource;

  @override
  Future<Household?> getCurrent() async {
    final model = await _datasource.getCurrent();
    return model?.toDomain();
  }

  @override
  Future<Household> invite(String email) async {
    final model = await _datasource.invite(email);
    return model.toDomain();
  }

  @override
  Future<Household> accept(String householdId) async {
    final model = await _datasource.accept(householdId);
    return model.toDomain();
  }

  @override
  Future<void> decline(String householdId) => _datasource.decline(householdId);

  @override
  Future<void> leave() => _datasource.leave();
}
