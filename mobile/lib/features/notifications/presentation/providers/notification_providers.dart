import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../data/repositories/notification_repository_impl.dart';
import '../../domain/entities/notification_item.dart';

part 'notification_providers.g.dart';

@riverpod
class NotificationList extends _$NotificationList {
  @override
  Future<List<NotificationItem>> build() =>
      ref.watch(notificationRepositoryProvider).getList();

  Future<void> markRead(String id) async {
    await ref.read(notificationRepositoryProvider).markRead(id);
    ref.invalidateSelf();
  }
}

@riverpod
Future<int> unreadNotificationCount(Ref ref) async {
  final items = await ref.watch(notificationListProvider.future);
  return items.where((n) => !n.read).length;
}
