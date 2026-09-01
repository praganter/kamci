import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../../domain/entities/member_embedded.dart';
import '../models/category_model.dart';
import '../models/create_category_request.dart';
import '../models/member_embedded_model.dart';

part 'category_remote_datasource.g.dart';

@riverpod
CategoryRemoteDatasource categoryRemoteDatasource(Ref ref) =>
    CategoryRemoteDatasource(ref.watch(dioClientProvider));

class CategoryRemoteDatasource {
  const CategoryRemoteDatasource(this._dio);
  final Dio _dio;

  Future<List<CategoryModel>> getAll() async {
    try {
      final response = await _dio.get(ApiConstants.categoryGetAll);
      final list = response.data as List<dynamic>;
      return list
          .map((e) => CategoryModel.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<CategoryModel> create(String name) async {
    try {
      final response = await _dio.post(
        ApiConstants.categorySave,
        data: {'name': name},
      );
      final data = Map<String, dynamic>.from(
        response.data as Map<String, dynamic>,
      );
      // Live backend'in create yanıtı hâlâ ownerId içermiyor (2026-09-01
      // doğrulandı) — placeholder CategoryModel'in required alanını karşılar,
      // repository katmanı gerçek değeri yazar. Backend eklerse putIfAbsent
      // dokunmaz.
      data.putIfAbsent('ownerId', () => '');
      return CategoryModel.fromJson(data);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<CategoryModel> update(String id, String name) async {
    try {
      final response = await _dio.post(
        ApiConstants.categoryUpdate,
        data: {'id': id, 'name': name},
      );
      final data = Map<String, dynamic>.from(
        response.data as Map<String, dynamic>,
      );
      // Same as create: live backend response ownerId içermiyor.
      data.putIfAbsent('ownerId', () => '');
      return CategoryModel.fromJson(data);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> delete(String id) async {
    try {
      await _dio.delete('${ApiConstants.categoryDelete}/$id');
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> addMember(
    String categoryId,
    String email,
    AccessLevel accessLevel,
  ) async {
    try {
      await _dio.post(
        '${ApiConstants.categoryMemberAdd}/$categoryId/member',
        data: AddMemberRequest(
          email: email,
          accessLevel: AccessLevelJson.fromDomain(accessLevel),
        ).toJson(),
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> addMembersBulk(
    List<String> categoryIds,
    String email,
    AccessLevel accessLevel,
  ) async {
    try {
      await _dio.post(
        ApiConstants.categoryMemberBulk,
        data: AddMembersBulkRequest(
          categoryIds: categoryIds,
          email: email,
          accessLevel: AccessLevelJson.fromDomain(accessLevel),
        ).toJson(),
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> removeMember(String categoryId, String userId) async {
    try {
      await _dio.delete(
        '${ApiConstants.categoryMemberRemove}/$categoryId/member/$userId',
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<int> migrateAndDelete(String id, String targetCategoryId) async {
    try {
      final response = await _dio.delete(
        '${ApiConstants.categoryMigrate}/$id/migrate',
        queryParameters: {'targetCategoryId': targetCategoryId},
      );
      return (response.data as Map<String, dynamic>)['migratedDebtCount']
          as int;
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  ApiException _mapException(DioException e) =>
      ApiException.fromDioException(e);
}
