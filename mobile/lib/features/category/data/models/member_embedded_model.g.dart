// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'member_embedded_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_MemberEmbeddedModel _$MemberEmbeddedModelFromJson(Map<String, dynamic> json) =>
    _MemberEmbeddedModel(
      userId: json['userId'] as String,
      accessLevel: $enumDecode(_$AccessLevelJsonEnumMap, json['accessLevel']),
    );

Map<String, dynamic> _$MemberEmbeddedModelToJson(
  _MemberEmbeddedModel instance,
) => <String, dynamic>{
  'userId': instance.userId,
  'accessLevel': _$AccessLevelJsonEnumMap[instance.accessLevel]!,
};

const _$AccessLevelJsonEnumMap = {
  AccessLevelJson.read: 'READ',
  AccessLevelJson.write: 'WRITE',
  AccessLevelJson.admin: 'ADMIN',
};
