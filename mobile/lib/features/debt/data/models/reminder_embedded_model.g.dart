// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reminder_embedded_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_ReminderEmbeddedModel _$ReminderEmbeddedModelFromJson(
  Map<String, dynamic> json,
) => _ReminderEmbeddedModel(
  enabled: json['enabled'] as bool? ?? false,
  channels:
      (json['channels'] as List<dynamic>?)?.map((e) => e as String).toList() ??
      const [],
  reminderDate: json['reminderDate'] as String?,
  reminderTime: json['reminderTime'] as String?,
);

Map<String, dynamic> _$ReminderEmbeddedModelToJson(
  _ReminderEmbeddedModel instance,
) => <String, dynamic>{
  'enabled': instance.enabled,
  'channels': instance.channels,
  if (instance.reminderDate case final value?) 'reminderDate': value,
  if (instance.reminderTime case final value?) 'reminderTime': value,
};
