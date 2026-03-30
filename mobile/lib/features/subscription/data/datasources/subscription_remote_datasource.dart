import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../models/subscription_model.dart';
import '../models/verify_receipt_request.dart';

part 'subscription_remote_datasource.g.dart';

@riverpod
SubscriptionRemoteDatasource subscriptionRemoteDatasource(Ref ref) =>
    SubscriptionRemoteDatasource(ref.watch(dioClientProvider));

class SubscriptionRemoteDatasource {
  const SubscriptionRemoteDatasource(this._dio);
  final Dio _dio;

  Future<SubscriptionModel> getStatus() async {
    try {
      final response = await _dio.get(ApiConstants.subscriptionStatus);
      return SubscriptionModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<SubscriptionModel> verifyReceipt(VerifyReceiptRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.subscriptionVerify,
        data: request.toJson(),
      );
      return SubscriptionModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  ApiException _mapException(DioException e) {
    final statusCode = e.response?.statusCode;
    if (statusCode == 401) return const UnauthorizedException();
    if (statusCode == 403) {
      final msg = e.response?.data?['message'] as String?;
      return PremiumLimitException(msg ?? 'Bu özellik premium üyeler içindir.');
    }
    if (statusCode == 404) return const NotFoundException();
    if (statusCode == 422) {
      final data = e.response?.data;
      final errors = data?['errors'] as List?;
      final message = errors != null && errors.isNotEmpty
          ? (errors.first['message'] as String?) ?? 'Doğrulama hatası.'
          : (data?['message'] as String?) ?? 'Doğrulama hatası.';
      return ServerException(message);
    }
    if (statusCode != null && statusCode >= 500) {
      final message = e.response?.data?['message'] as String?;
      return ServerException(message ?? 'Sunucu hatası oluştu.');
    }
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.connectionError) {
      return const NetworkException();
    }
    return const UnknownException();
  }
}
