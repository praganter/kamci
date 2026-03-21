import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/member_embedded.dart';

part 'member_embedded_model.freezed.dart';
part 'member_embedded_model.g.dart';

@JsonEnum(alwaysCreate: true)
enum AccessLevelJson {
  @JsonValue('READ') read,
  @JsonValue('WRITE') write,
  @JsonValue('ADMIN') admin;

  AccessLevel toDomain() => switch (this) {
    AccessLevelJson.read => AccessLevel.read,
    AccessLevelJson.write => AccessLevel.write,
    AccessLevelJson.admin => AccessLevel.admin,
  };

  static AccessLevelJson fromDomain(AccessLevel level) => switch (level) {
    AccessLevel.read => AccessLevelJson.read,
    AccessLevel.write => AccessLevelJson.write,
    AccessLevel.admin => AccessLevelJson.admin,
  };
}

@freezed
abstract class MemberEmbeddedModel with _$MemberEmbeddedModel {
  const factory MemberEmbeddedModel({
    required String userId,
    required AccessLevelJson accessLevel,
  }) = _MemberEmbeddedModel;

  factory MemberEmbeddedModel.fromJson(Map<String, dynamic> json) =>
      _$MemberEmbeddedModelFromJson(json);
}

extension MemberEmbeddedModelMapper on MemberEmbeddedModel {
  MemberEmbedded toDomain() => MemberEmbedded(
    userId: userId,
    accessLevel: accessLevel.toDomain(),
  );
}
