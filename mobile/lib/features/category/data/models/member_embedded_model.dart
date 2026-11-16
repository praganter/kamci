import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/member_embedded.dart';

part 'member_embedded_model.freezed.dart';
part 'member_embedded_model.g.dart';

@JsonEnum(alwaysCreate: true)
enum AccessLevelJson {
  @JsonValue('READ')
  read,
  @JsonValue('WRITE')
  write,
  @JsonValue('EDIT')
  edit,
  @JsonValue('DELETE')
  delete;

  AccessLevel toDomain() => switch (this) {
    AccessLevelJson.read => AccessLevel.read,
    AccessLevelJson.write => AccessLevel.write,
    AccessLevelJson.edit => AccessLevel.edit,
    AccessLevelJson.delete => AccessLevel.admin,
  };

  static AccessLevelJson fromDomain(AccessLevel level) => switch (level) {
    AccessLevel.read => AccessLevelJson.read,
    AccessLevel.write => AccessLevelJson.write,
    AccessLevel.edit => AccessLevelJson.edit,
    AccessLevel.admin => AccessLevelJson.delete,
  };
}

@freezed
sealed class MemberEmbeddedModel with _$MemberEmbeddedModel {
  const factory MemberEmbeddedModel({
    String? userId,
    required AccessLevelJson accessLevel,
    String? email,
  }) = _MemberEmbeddedModel;

  factory MemberEmbeddedModel.fromJson(Map<String, dynamic> json) =>
      _$MemberEmbeddedModelFromJson(json);
}

extension MemberEmbeddedModelMapper on MemberEmbeddedModel {
  MemberEmbedded toDomain() => MemberEmbedded(
    userId: userId,
    accessLevel: accessLevel.toDomain(),
    email: email,
  );
}
