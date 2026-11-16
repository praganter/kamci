import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/repositories/auth_repository_impl.dart';

class ChangePasswordNotifier extends StateNotifier<AsyncValue<void>> {
  ChangePasswordNotifier(this._repository) : super(const AsyncData(null));

  final dynamic _repository;

  Future<bool> changePassword(
    String currentPassword,
    String newPassword,
  ) async {
    state = const AsyncLoading();
    try {
      await _repository.changePassword(currentPassword, newPassword);
      state = const AsyncData(null);
      return true;
    } catch (e, st) {
      state = AsyncError(e, st);
      return false;
    }
  }

  void reset() => state = const AsyncData(null);
}

final changePasswordProvider =
    StateNotifierProvider.autoDispose<ChangePasswordNotifier, AsyncValue<void>>(
      (ref) => ChangePasswordNotifier(ref.watch(authRepositoryProvider)),
    );
