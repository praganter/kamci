import 'package:freezed_annotation/freezed_annotation.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../domain/entities/reminder_embedded.dart';

part 'reminder_embedded_model.freezed.dart';
part 'reminder_embedded_model.g.dart';

@freezed
abstract class ReminderEmbeddedModel with _$ReminderEmbeddedModel {
  const factory ReminderEmbeddedModel({
    @Default(false) bool enabled,
    @Default([]) List<String> channels,
    String? reminderDate,
    String? reminderTime,
  }) = _ReminderEmbeddedModel;

  factory ReminderEmbeddedModel.fromJson(Map<String, dynamic> json) =>
      _$ReminderEmbeddedModelFromJson(json);
}

extension ReminderEmbeddedModelMapper on ReminderEmbeddedModel {
  ReminderEmbedded toDomain() => ReminderEmbedded(
    enabled: enabled,
    channels: channels.isEmpty ? null : channels,
    reminderDate: DateFormatter.parseApiDate(reminderDate),
    reminderTime: reminderTime,
  );
}

extension ReminderEmbeddedFromDomain on ReminderEmbedded {
  ReminderEmbeddedModel toModel() => ReminderEmbeddedModel(
    enabled: enabled,
    channels: channels ?? [],
    reminderDate: reminderDate != null
        ? DateFormatter.toApiFormat(reminderDate!)
        : null,
    reminderTime: reminderTime,
  );
}
