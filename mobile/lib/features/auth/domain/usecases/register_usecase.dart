import '../entities/auth_tokens.dart';
import '../repositories/auth_repository.dart';

class RegisterUsecase {
  const RegisterUsecase(this._repository);
  final AuthRepository _repository;

  Future<AuthTokens> call(
    String email,
    String password, {
    bool premium = false,
  }) => _repository.register(email, password, premium: premium);
}
