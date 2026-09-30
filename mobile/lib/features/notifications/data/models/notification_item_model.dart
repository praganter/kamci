import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/notification_item.dart';

part 'notification_item_model.freezed.dart';
part 'notification_item_model.g.dart';

@freezed
sealed class NotificationItemModel with _$NotificationItemModel {
  const factory NotificationItemModel({
    required String id,
    required String type,
    required String title,
    required String body,
    String? entityId,
    @Default(false) bool read,
    String? createdDate,
  }) = _NotificationItemModel;

  factory NotificationItemModel.fromJson(Map<String, dynamic> json) =>
      _$NotificationItemModelFromJson(json);
}

extension NotificationItemModelMapper on NotificationItemModel {
  NotificationItem toDomain() => NotificationItem(
    id: id,
    type: type,
    title: title,
    body: body,
    entityId: entityId,
    read: read,
    createdAt: createdDate != null ? DateTime.tryParse(createdDate!) : null,
  );
}
