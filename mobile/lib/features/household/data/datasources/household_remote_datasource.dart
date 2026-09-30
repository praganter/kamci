import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../models/household_model.dart';

part 'household_remote_datasource.g.dart';

@riverpod
HouseholdRemoteDatasource householdRemoteDatasource(Ref ref) =>
    HouseholdRemoteDatasource(ref.watch(dioClientProvider));

class HouseholdRemoteDatasource {
  const HouseholdRemoteDatasource(this._dio);
  final Dio _dio;

  /// Backend returns 204 No Content when the user has no household.
  Future<HouseholdModel?> getCurrent() async {
    try {
      final response = await _dio.get(ApiConstants.household);
      if (response.statusCode == 204 || response.data == null) return null;
      return HouseholdModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<HouseholdModel> invite(String email) async {
    try {
      final response = await _dio.post(
        ApiConstants.householdInvite,
        data: {'email': email},
      );
      return HouseholdModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<HouseholdModel> accept(String householdId) async {
    try {
      final response = await _dio.post(
        '${ApiConstants.householdAccept}/$householdId',
      );
      return HouseholdModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> decline(String householdId) async {
    try {
      await _dio.post('${ApiConstants.householdDecline}/$householdId');
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> leave() async {
    try {
      await _dio.delete(ApiConstants.household);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  ApiException _mapException(DioException e) => ApiException.fromDioException(e);
}
