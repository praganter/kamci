import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../models/create_debt_request.dart';
import '../models/debt_model.dart';

part 'debt_remote_datasource.g.dart';

@riverpod
DebtRemoteDatasource debtRemoteDatasource(Ref ref) =>
    DebtRemoteDatasource(ref.watch(dioClientProvider));

class DebtRemoteDatasource {
  const DebtRemoteDatasource(this._dio);
  final Dio _dio;

  Future<List<DebtModel>> getAll() async {
    try {
      final response = await _dio.get(ApiConstants.debtGet);
      final data = response.data as Map<String, dynamic>;
      final categories = data['categories'] as List<dynamic>;
      final debts = <DebtModel>[];
      for (final category in categories) {
        final categoryDebts = (category['debts'] as List<dynamic>?) ?? [];
        for (final debt in categoryDebts) {
          debts.add(DebtModel.fromJson(debt as Map<String, dynamic>));
        }
      }
      return debts;
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<List<({String categoryId, String categoryName, bool owned, List<DebtModel> debts})>>
      getGrouped() async {
    try {
      final response = await _dio.get(ApiConstants.debtGet);
      final data = response.data as Map<String, dynamic>;
      final categories = data['categories'] as List<dynamic>;
      return categories.map((c) {
        final rawDebts = (c['debts'] as List<dynamic>?) ?? [];
        return (
          categoryId: c['categoryId'] as String,
          categoryName: c['categoryName'] as String,
          owned: c['owned'] as bool? ?? true,
          debts: rawDebts
              .map((d) => DebtModel.fromJson(d as Map<String, dynamic>))
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

  Future<DebtModel> pay(String id) async {
    try {
      final response = await _dio.post('${ApiConstants.debtPay}/$id');
      final debt = (response.data as Map<String, dynamic>)['debt'] as Map<String, dynamic>;
      return DebtModel.fromJson(debt);
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

  ApiException _mapException(DioException e) {
    final statusCode = e.response?.statusCode;
    if (statusCode == 401) return const UnauthorizedException();
    if (statusCode == 403) return const ForbiddenException();
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
