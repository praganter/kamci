import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../models/create_debt_request.dart';
import '../models/debt_model.dart';
import '../models/debt_summary_model.dart';

part 'debt_remote_datasource.g.dart';

@riverpod
DebtRemoteDatasource debtRemoteDatasource(Ref ref) =>
    DebtRemoteDatasource(ref.watch(dioClientProvider));

class DebtRemoteDatasource {
  const DebtRemoteDatasource(this._dio);
  final Dio _dio;

  Future<
    List<
      ({
        String categoryId,
        String categoryName,
        bool owned,
        String? ownerUserId,
        List<DebtSummaryModel> debts,
      })
    >
  >
  getGrouped({required String month, required bool isOwner}) async {
    try {
      final response = await _dio.get(
        ApiConstants.debtGet,
        queryParameters: {'month': month, 'isOwner': isOwner},
      );
      final data = response.data as Map<String, dynamic>;
      final categories = data['categories'] as List<dynamic>;
      return categories.map((c) {
        final rawDebts = (c['debts'] as List<dynamic>?) ?? [];
        return (
          categoryId: c['categoryId'] as String,
          categoryName: c['categoryName'] as String,
          owned: c['owned'] as bool? ?? true,
          ownerUserId: c['ownerUserId'] as String?,
          debts: rawDebts
              .map((d) => DebtSummaryModel.fromJson(d as Map<String, dynamic>))
              .toList(),
        );
      }).toList();
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> create(CreateDebtRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.debtSave,
        data: request.toJson(),
      );
      final debt = (response.data as Map<String, dynamic>)['debt'] as Map<String, dynamic>;
      return DebtModel.fromJson(debt);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> update(CreateDebtRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.debtUpdate,
        data: request.toJson(),
      );
      final debt = (response.data as Map<String, dynamic>)['debt'] as Map<String, dynamic>;
      return DebtModel.fromJson(debt);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> pay(String id, {double? amountPaid}) async {
    try {
      final response = await _dio.post(
        '${ApiConstants.debtPay}/$id',
        data: amountPaid != null ? {'paidAmount': amountPaid} : null,
      );
      final debt = (response.data as Map<String, dynamic>)['debt'] as Map<String, dynamic>;
      return DebtModel.fromJson(debt);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> skip(String id) async {
    try {
      final response = await _dio.post('${ApiConstants.debtSkip}/$id');
      final debt = (response.data as Map<String, dynamic>)['debt'] as Map<String, dynamic>;
      return DebtModel.fromJson(debt);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> bulkPay(
    String id,
    List<Map<String, dynamic>> payments,
  ) async {
    try {
      final response = await _dio.post(
        '${ApiConstants.debtBulkPay}/$id/bulk',
        data: payments,
      );
      final debt =
          (response.data as Map<String, dynamic>)['debt'] as Map<String, dynamic>;
      return DebtModel.fromJson(debt);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<Map<String, dynamic>> getSummary(String month) async {
    try {
      final response = await _dio.get(
        ApiConstants.debtSummary,
        queryParameters: {'month': month},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> getById(String id) async {
    try {
      final response = await _dio.get('${ApiConstants.debtGetById}/$id');
      return DebtModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> delete(String id) async {
    try {
      await _dio.delete('${ApiConstants.debtDelete}/$id');
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<List<Map<String, dynamic>>> getPaymentHistory(
    String id,
    DateTime from,
    DateTime to,
  ) async {
    try {
      final response = await _dio.get(
        '${ApiConstants.debtPayments}/$id/payments',
        queryParameters: {
          'from': from.toIso8601String().substring(0, 19),
          'to': to.toIso8601String().substring(0, 19),
        },
      );
      final list = response.data as List<dynamic>;
      return list.map((e) => e as Map<String, dynamic>).toList();
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  ApiException _mapException(DioException e) => ApiException.fromDioException(e);
}
