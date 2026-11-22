import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/subscription.dart';

part 'subscription_model.freezed.dart';
part 'subscription_model.g.dart';

@freezed
sealed class SubscriptionModel with _$SubscriptionModel {
  const factory SubscriptionModel({
    required String plan,
    required String status,
    @JsonKey(name: 'expiryDate', fromJson: _expiryFromJson)
    DateTime? expiresDate,
    String? accessToken,
    String? refreshToken,
  }) = _SubscriptionModel;

  factory SubscriptionModel.fromJson(Map<String, dynamic> json) =>
      _$SubscriptionModelFromJson(json);
}

DateTime? _expiryFromJson(Object? value) {
  if (value is! String) return null;
  final parsed = DateTime.tryParse(value);
  if (parsed == null) return null;
  if (parsed.isUtc) return parsed.toLocal();
  return DateTime.utc(
    parsed.year,
    parsed.month,
    parsed.day,
    parsed.hour,
    parsed.minute,
    parsed.second,
    parsed.millisecond,
    parsed.microsecond,
  ).toLocal();
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
