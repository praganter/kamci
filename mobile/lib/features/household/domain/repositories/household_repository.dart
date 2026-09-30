import '../entities/household.dart';

abstract interface class HouseholdRepository {
  /// Null if the user has no household (backend returns 204 No Content).
  Future<Household?> getCurrent();
  Future<Household> invite(String email);
  Future<Household> accept(String householdId);
  Future<void> decline(String householdId);
  Future<void> leave();
}
