import '../entities/auth_tokens.dart';

abstract interface class AuthRepository {
  Future<AuthTokens> login(String email, String password);
  Future<AuthTokens> register(String email, String password, {bool premium = false});
  Future<AuthTokens> refreshToken(String refreshToken);
  Future<void> forgotPassword(String email);
  Future<void> resetPassword(String token, String newPassword);
  Future<void> changePassword(String currentPassword, String newPassword);
}
