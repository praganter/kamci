import '../entities/auth_tokens.dart';
import '../repositories/auth_repository.dart';

class LoginUsecase {
  const LoginUsecase(this._repository);
  final AuthRepository _repository;

  Future<AuthTokens> call(String email, String password) =>
      _repository.login(email, password);
}
