import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/household.dart';

part 'household_model.freezed.dart';
part 'household_model.g.dart';

@JsonEnum(alwaysCreate: true)
enum HouseholdStatusJson {
  @JsonValue('PENDING')
  pending,
  @JsonValue('ACCEPTED')
  accepted;

  HouseholdStatus toDomain() => switch (this) {
    HouseholdStatusJson.pending => HouseholdStatus.pending,
    HouseholdStatusJson.accepted => HouseholdStatus.accepted,
  };
}

@freezed
sealed class HouseholdModel with _$HouseholdModel {
  const factory HouseholdModel({
    required String id,
    required String ownerId,
    required String ownerEmail,
    required String partnerId,
    required String partnerEmail,
    required HouseholdStatusJson status,
    @Default(false) bool owner,
  }) = _HouseholdModel;

  factory HouseholdModel.fromJson(Map<String, dynamic> json) =>
      _$HouseholdModelFromJson(json);
}

extension HouseholdModelMapper on HouseholdModel {
  Household toDomain() => Household(
    id: id,
    ownerId: ownerId,
    ownerEmail: ownerEmail,
    partnerId: partnerId,
    partnerEmail: partnerEmail,
    status: status.toDomain(),
    isOwner: owner,
  );
}
