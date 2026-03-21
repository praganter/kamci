import '../repositories/auth_repository.dart';

class ResetPasswordUsecase {
  const ResetPasswordUsecase(this._repository);
  final AuthRepository _repository;

  Future<void> call(String token, String newPassword) =>
      _repository.resetPassword(token, newPassword);
}
