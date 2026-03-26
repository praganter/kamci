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
      return list.map((e) => CategoryModel.fromJson(e as Map<String, dynamic>)).toList();
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
      final data = Map<String, dynamic>.from(response.data as Map<String, dynamic>);
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
      return CategoryModel.fromJson(response.data as Map<String, dynamic>);
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

  Future<CategoryModel> addMember(
    String categoryId,
    String userId,
    AccessLevel accessLevel,
  ) async {
    try {
      final response = await _dio.post(
        '/category/$categoryId/member',
        data: AddMemberRequest(
          userId: userId,
          accessLevel: AccessLevelJson.fromDomain(accessLevel),
        ).toJson(),
      );
      return CategoryModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<CategoryModel> addMembersBulk(
    String categoryId,
    List<({String userId, AccessLevel accessLevel})> members,
  ) async {
    try {
      final response = await _dio.post(
        ApiConstants.categoryMemberBulk,
        data: AddMembersBulkRequest(
          categoryId: categoryId,
          members: members
              .map((m) => AddMemberRequest(
                    userId: m.userId,
                    accessLevel: AccessLevelJson.fromDomain(m.accessLevel),
                  ))
              .toList(),
        ).toJson(),
      );
      return CategoryModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<CategoryModel> removeMember(String categoryId, String userId) async {
    try {
      final response = await _dio.delete('/category/$categoryId/member/$userId');
      return CategoryModel.fromJson(response.data as Map<String, dynamic>);
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
