import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/notification_item.dart';
import '../../domain/repositories/notification_repository.dart';
import '../datasources/notification_remote_datasource.dart';
import '../models/notification_item_model.dart';

part 'notification_repository_impl.g.dart';

@riverpod
NotificationRepository notificationRepository(Ref ref) =>
    NotificationRepositoryImpl(ref.watch(notificationRemoteDatasourceProvider));

class NotificationRepositoryImpl implements NotificationRepository {
  const NotificationRepositoryImpl(this._datasource);
  final NotificationRemoteDatasource _datasource;

  @override
  Future<List<NotificationItem>> getList() async {
    final models = await _datasource.getList();
    return models.map((m) => m.toDomain()).toList();
  }

  @override
  Future<void> markRead(String id) => _datasource.markRead(id);
}
