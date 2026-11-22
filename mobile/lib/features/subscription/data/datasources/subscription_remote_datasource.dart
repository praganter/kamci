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

  ApiException _mapException(DioException e) =>
      ApiException.fromDioException(e);
}
