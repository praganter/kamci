import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/subscription.dart';

part 'subscription_model.freezed.dart';
part 'subscription_model.g.dart';

@freezed
abstract class SubscriptionModel with _$SubscriptionModel {
  const factory SubscriptionModel({
    required String plan,
    required String status,
    @JsonKey(name: 'expiryDate') DateTime? expiresDate,
    String? accessToken,
    String? refreshToken,
  }) = _SubscriptionModel;

  factory SubscriptionModel.fromJson(Map<String, dynamic> json) =>
      _$SubscriptionModelFromJson(json);
}

extension SubscriptionModelX on SubscriptionModel {
  Subscription toDomain() => Subscription(
    plan: plan,
    status: status,
    expiresDate: expiresDate,
    accessToken: accessToken,
    refreshToken: refreshToken,
  );
}
