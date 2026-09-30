import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/network/dio_client.dart';
import '../models/notification_item_model.dart';

part 'notification_remote_datasource.g.dart';

@riverpod
NotificationRemoteDatasource notificationRemoteDatasource(Ref ref) =>
    NotificationRemoteDatasource(ref.watch(dioClientProvider));

class NotificationRemoteDatasource {
  const NotificationRemoteDatasource(this._dio);
  final Dio _dio;

  /// First page only (backend default page=0/size=20) — sayfalama UI'da
  /// henüz yok, kapsam dışı bırakıldı (bkz. HANDOFF 2026-09-22).
  Future<List<NotificationItemModel>> getList() async {
    try {
      final response = await _dio.get(ApiConstants.notificationList);
      final data = response.data as Map<String, dynamic>;
      final content = data['content'] as List<dynamic>? ?? [];
      return content
          .map(
            (e) => NotificationItemModel.fromJson(e as Map<String, dynamic>),
          )
          .toList();
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  Future<void> markRead(String id) async {
    try {
      await _dio.patch('${ApiConstants.notificationRead}/$id/read');
    } on DioException catch (e) {
      throw _mapException(e);
    }
  }

  ApiException _mapException(DioException e) => ApiException.fromDioException(e);
}
