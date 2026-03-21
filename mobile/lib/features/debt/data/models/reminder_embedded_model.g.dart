// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'reminder_embedded_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_ReminderEmbeddedModel _$ReminderEmbeddedModelFromJson(
  Map<String, dynamic> json,
) => _ReminderEmbeddedModel(
  enabled: json['enabled'] as bool? ?? false,
  frequency: json['frequency'] as String?,
);

Map<String, dynamic> _$ReminderEmbeddedModelToJson(
  _ReminderEmbeddedModel instance,
) => <String, dynamic>{
  'enabled': instance.enabled,
  if (instance.frequency case final value?) 'frequency': value,
};
