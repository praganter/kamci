import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../models/income_model.dart';
import '../models/save_income_request.dart';

part 'income_remote_datasource.g.dart';

@riverpod
IncomeRemoteDatasource incomeRemoteDatasource(IncomeRemoteDatasourceRef ref) =>
    IncomeRemoteDatasource(ref.watch(dioClientProvider));

class IncomeRemoteDatasource {
  const IncomeRemoteDatasource(this._dio);
  final Dio _dio;

  Future<IncomeModel> save(SaveIncomeRequest request) async {
    try {
      final response = await _dio.post(
        ApiConstants.incomeSave,
        data: request.toJson(),
      );
      return IncomeModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<List<IncomeModel>> getHistory() async {
    try {
      final response = await _dio.get(ApiConstants.incomeHistory);
      final list = response.data as List<dynamic>;
      return list
          .map((e) => IncomeModel.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<IncomeModel> getCurrent() async {
    try {
      final response = await _dio.get(ApiConstants.incomeCurrent);
      return IncomeModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<void> delete(String id) async {
    try {
      await _dio.delete('${ApiConstants.incomeDelete}/$id');
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }
}
