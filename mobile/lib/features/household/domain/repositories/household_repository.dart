import '../entities/household.dart';

abstract interface class HouseholdRepository {
  Future<Household?> getCurrent();
  Future<Household> invite(String email);
  Future<Household> accept(String householdId);
  Future<void> decline(String householdId);
  Future<void> leave();
}
