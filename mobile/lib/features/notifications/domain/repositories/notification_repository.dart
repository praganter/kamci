import '../entities/notification_item.dart';

abstract interface class NotificationRepository {
  Future<List<NotificationItem>> getList();
  Future<void> markRead(String id);
}
