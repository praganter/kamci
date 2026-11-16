import '../entities/auth_tokens.dart';
import '../entities/user_detail.dart';

abstract interface class AuthRepository {
  Future<AuthTokens> login(String email, String password);
  Future<AuthTokens> register(String email, String password);
  Future<AuthTokens> refreshToken(String refreshToken);
  Future<void> logout(String refreshToken);
  Future<void> deleteAccount();
  Future<void> forgotPassword(String email);
  Future<void> resetPassword(String token, String newPassword);
  Future<void> changePassword(String currentPassword, String newPassword);
  Future<UserDetail> userDetail();
}
