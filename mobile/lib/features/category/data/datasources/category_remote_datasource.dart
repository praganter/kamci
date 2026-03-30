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
      final data = Map<String, dynamic>.from(response.data as Map<String, dynamic>);
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
        '/category/$categoryId/member',
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
    String categoryId,
    List<({String email, AccessLevel accessLevel})> members,
  ) async {
    try {
      await _dio.post(
        ApiConstants.categoryMemberBulk,
        data: AddMembersBulkRequest(
          categoryId: categoryId,
          members: members
              .map((m) => AddMemberRequest(
                    email: m.email,
                    accessLevel: AccessLevelJson.fromDomain(m.accessLevel),
                  ))
              .toList(),
        ).toJson(),
      );
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> removeMember(String categoryId, String userId) async {
    try {
      await _dio.delete('/category/$categoryId/member/$userId');
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
    if (statusCode == 409) {
      final msg = e.response?.data?['message'] as String?;
      return BusinessException(msg ?? 'Bu işlem şu an gerçekleştirilemiyor.');
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
