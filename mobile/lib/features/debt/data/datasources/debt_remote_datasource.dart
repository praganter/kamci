import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/utils/date_formatter.dart';
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
          categoryId: c['categoryId'] as String? ?? '',
          categoryName: c['categoryName'] as String? ?? '',
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
      return DebtModel.fromJson(_extractDebt(response.data));
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
      return DebtModel.fromJson(_extractDebt(response.data));
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> pay(
    String id, {
    double? amountPaid,
    DateTime? paidTime,
  }) async {
    try {
      final body = <String, dynamic>{
        'amountPaid': ?amountPaid,
        if (paidTime != null)
          'paidTime': DateFormatter.toApiDateTimeFormat(paidTime),
      };
      final response = await _dio.post(
        '${ApiConstants.debtPay}/$id',
        data: body.isEmpty ? null : body,
      );
      return DebtModel.fromJson(_extractDebt(response.data));
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<DebtModel> skip(String id) async {
    try {
      final response = await _dio.post('${ApiConstants.debtSkip}/$id');
      return DebtModel.fromJson(_extractDebt(response.data));
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
      return DebtModel.fromJson(_extractDebt(response.data));
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Map<String, dynamic> _extractDebt(dynamic data) {
    final map = data as Map<String, dynamic>;
    final debt = map['debt'];
    if (debt == null) throw const FormatException('Missing "debt" in response');
    return debt as Map<String, dynamic>;
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

  ApiException _mapException(DioException e) =>
      ApiException.fromDioException(e);
}
