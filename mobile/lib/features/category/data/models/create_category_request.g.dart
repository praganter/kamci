// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'create_category_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_CreateCategoryRequest _$CreateCategoryRequestFromJson(
  Map<String, dynamic> json,
) => _CreateCategoryRequest(
  id: json['id'] as String,
  name: json['name'] as String,
);

Map<String, dynamic> _$CreateCategoryRequestToJson(
  _CreateCategoryRequest instance,
) => <String, dynamic>{'id': instance.id, 'name': instance.name};

_AddMemberRequest _$AddMemberRequestFromJson(Map<String, dynamic> json) =>
    _AddMemberRequest(
      userId: json['userId'] as String,
      accessLevel: $enumDecode(_$AccessLevelJsonEnumMap, json['accessLevel']),
    );

Map<String, dynamic> _$AddMemberRequestToJson(_AddMemberRequest instance) =>
    <String, dynamic>{
      'userId': instance.userId,
      'accessLevel': _$AccessLevelJsonEnumMap[instance.accessLevel]!,
    };

const _$AccessLevelJsonEnumMap = {
  AccessLevelJson.read: 'READ',
  AccessLevelJson.write: 'WRITE',
  AccessLevelJson.admin: 'ADMIN',
};

_AddMembersBulkRequest _$AddMembersBulkRequestFromJson(
  Map<String, dynamic> json,
) => _AddMembersBulkRequest(
  categoryId: json['categoryId'] as String,
  members: (json['members'] as List<dynamic>)
      .map((e) => AddMemberRequest.fromJson(e as Map<String, dynamic>))
      .toList(),
);

Map<String, dynamic> _$AddMembersBulkRequestToJson(
  _AddMembersBulkRequest instance,
) => <String, dynamic>{
  'categoryId': instance.categoryId,
  'members': instance.members.map((e) => e.toJson()).toList(),
};
