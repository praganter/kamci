import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/reminder_embedded.dart';

part 'reminder_embedded_model.freezed.dart';
part 'reminder_embedded_model.g.dart';

@freezed
abstract class ReminderEmbeddedModel with _$ReminderEmbeddedModel {
  const factory ReminderEmbeddedModel({
    @Default(false) bool enabled,
    String? frequency,
  }) = _ReminderEmbeddedModel;

  factory ReminderEmbeddedModel.fromJson(Map<String, dynamic> json) =>
      _$ReminderEmbeddedModelFromJson(json);
}

extension ReminderEmbeddedModelMapper on ReminderEmbeddedModel {
  ReminderEmbedded toDomain() =>
      ReminderEmbedded(enabled: enabled, frequency: frequency);
}
