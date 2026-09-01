import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/auth_tokens.dart';
import '../../domain/entities/user_detail.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../models/login_request.dart';
import '../models/register_request.dart';

part 'auth_repository_impl.g.dart';

@riverpod
AuthRepository authRepository(Ref ref) {
  return AuthRepositoryImpl(ref.watch(authRemoteDatasourceProvider));
}

class AuthRepositoryImpl implements AuthRepository {
  const AuthRepositoryImpl(this._datasource);
  final AuthRemoteDatasource _datasource;

  @override
  Future<AuthTokens> login(String email, String password) async {
    final response = await _datasource.login(
      LoginRequest(email: email, password: password),
    );
    return AuthTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    );
  }

  @override
  Future<AuthTokens> register(
    String email,
    String password, {
    bool premium = false,
  }) async {
    final response = await _datasource.register(
      RegisterRequest(email: email, password: password, premium: premium),
    );
    return AuthTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    );
  }

  @override
  Future<AuthTokens> refreshToken(String refreshToken) async {
    final response = await _datasource.refreshToken(refreshToken);
    return AuthTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    );
  }

  @override
  Future<void> logout(String refreshToken) =>
      _datasource.logout(refreshToken);

  @override
  Future<void> forgotPassword(String email) =>
      _datasource.forgotPassword(email);

  @override
  Future<void> resetPassword(String token, String newPassword) =>
      _datasource.resetPassword(token, newPassword);

  @override
  Future<void> changePassword(String currentPassword, String newPassword) =>
      _datasource.changePassword(currentPassword, newPassword);

  @override
  Future<UserDetail> userDetail() => _datasource.userDetail();
}
