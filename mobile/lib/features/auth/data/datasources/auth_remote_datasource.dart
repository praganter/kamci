import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../../domain/entities/user_detail.dart';
import '../models/auth_response.dart';
import '../models/login_request.dart';
import '../models/register_request.dart';

part 'auth_remote_datasource.g.dart';

@riverpod
AuthRemoteDatasource authRemoteDatasource(Ref ref) {
  return AuthRemoteDatasource(
    ref.watch(refreshDioProvider),
    ref.watch(dioClientProvider),
  );
}

class AuthRemoteDatasource {
  const AuthRemoteDatasource(this._dio, this._authDio);

  /// Unauthenticated Dio (no AuthInterceptor — login, register, refresh)
  final Dio _dio;

  /// Authenticated Dio (AuthInterceptor adds Bearer token)
  final Dio _authDio;

  Future<AuthResponse> login(LoginRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.login,
        data: request.toJson(),
      );
      return AuthResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<AuthResponse> register(RegisterRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.register,
        data: request.toJson(),
      );
      return AuthResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<AuthResponse> refreshToken(String refreshToken) async {
    try {
      final response = await _dio.post(
        ApiConstants.refreshToken,
        options: Options(headers: {'Authorization': 'Bearer $refreshToken'}),
      );
      return AuthResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  /// Hard delete — geri dönüşsüz. JWT korumalı (access token interceptor'dan).
  Future<void> deleteAccount() async {
    try {
      await _dio.delete(ApiConstants.deleteAccount);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  /// Server-side refresh-token blacklist. Gateway'de public endpoint;
  /// refresh token'ı `Authorization: Bearer` ile gönderiyoruz (refreshToken ile
  /// aynı sözleşme — hem gateway hem auth-service "Bearer " prefix'i şart koşar).
  Future<void> logout(String refreshToken) async {
    try {
      await _dio.post(
        ApiConstants.logout,
        options: Options(headers: {'Authorization': 'Bearer $refreshToken'}),
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> forgotPassword(String email) async {
    try {
      await _dio.post(ApiConstants.forgetPassword, data: {'email': email});
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> resetPassword(String token, String newPassword) async {
    try {
      await _dio.post(
        ApiConstants.resetPassword,
        data: {'token': token, 'newPassword': newPassword},
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> changePassword(
    String currentPassword,
    String newPassword,
  ) async {
    try {
      await _authDio.post(
        ApiConstants.changePassword,
        data: {'currentPassword': currentPassword, 'newPassword': newPassword},
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<UserDetail> userDetail() async {
    try {
      final response = await _authDio.get(ApiConstants.userDetail);
      final data = response.data as Map<String, dynamic>;
      return UserDetail(
        email: data['email'] as String,
        premium: data['premium'] as bool? ?? false,
        createdAt: DateTime.parse(data['createdAt'] as String),
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  ApiException _mapException(DioException e) =>
      ApiException.fromDioException(e);
}
