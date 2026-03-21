import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/debt.dart';
import 'payment_embedded_model.dart';
import 'reminder_embedded_model.dart';

part 'debt_model.freezed.dart';
part 'debt_model.g.dart';

@JsonEnum(alwaysCreate: true)
enum PaymentPeriodTypeJson {
  @JsonValue('DAILY') daily,
  @JsonValue('WEEKLY') weekly,
  @JsonValue('MONTHLY') monthly,
  @JsonValue('QUARTERLY') quarterly,
  @JsonValue('YEARLY') yearly;

  PaymentPeriodType toDomain() => switch (this) {
    PaymentPeriodTypeJson.daily => PaymentPeriodType.daily,
    PaymentPeriodTypeJson.weekly => PaymentPeriodType.weekly,
    PaymentPeriodTypeJson.monthly => PaymentPeriodType.monthly,
    PaymentPeriodTypeJson.quarterly => PaymentPeriodType.quarterly,
    PaymentPeriodTypeJson.yearly => PaymentPeriodType.yearly,
  };

  static PaymentPeriodTypeJson fromDomain(PaymentPeriodType t) => switch (t) {
    PaymentPeriodType.daily => PaymentPeriodTypeJson.daily,
    PaymentPeriodType.weekly => PaymentPeriodTypeJson.weekly,
    PaymentPeriodType.monthly => PaymentPeriodTypeJson.monthly,
    PaymentPeriodType.quarterly => PaymentPeriodTypeJson.quarterly,
    PaymentPeriodType.yearly => PaymentPeriodTypeJson.yearly,
  };
}

@JsonEnum(alwaysCreate: true)
enum DebtStatusJson {
  @JsonValue('ACTIVE') active,
  @JsonValue('INACTIVE') inactive,
  @JsonValue('COMPLETED') completed;

  DebtStatus toDomain() => switch (this) {
    DebtStatusJson.active => DebtStatus.active,
    DebtStatusJson.inactive => DebtStatus.inactive,
    DebtStatusJson.completed => DebtStatus.completed,
  };

  static DebtStatusJson fromDomain(DebtStatus s) => switch (s) {
    DebtStatus.active => DebtStatusJson.active,
    DebtStatus.inactive => DebtStatusJson.inactive,
    DebtStatus.completed => DebtStatusJson.completed,
  };
}

@freezed
abstract class DebtModel with _$DebtModel {
  const factory DebtModel({
    required String id,
    required String name,
    required String ownerId,
    String? categoryId,
    String? description,
    required String startDate,
    String? endDate,
    required PaymentPeriodTypeJson paymentPeriodType,
    @JsonKey(fromJson: _amountFromJson) required double amount,
    @Default(DebtStatusJson.active) DebtStatusJson status,
    @Default([]) List<PaymentEmbeddedModel> payments,
    ReminderEmbeddedModel? reminder,
    String? nextReminderDate,
    @JsonKey(name: 'createdDate') String? createdAt,
    @JsonKey(name: 'updatedDate') String? updatedAt,
  }) = _DebtModel;

  factory DebtModel.fromJson(Map<String, dynamic> json) =>
      _$DebtModelFromJson(json);
}

double _amountFromJson(dynamic value) {
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0.0;
  return 0.0;
}

extension DebtModelMapper on DebtModel {
  Debt toDomain() => Debt(
    id: id,
    name: name,
    ownerId: ownerId,
    categoryId: categoryId,
    description: description,
    startDate: DateTime.parse(startDate),
    endDate: endDate != null ? DateTime.parse(endDate!) : null,
    paymentPeriodType: paymentPeriodType.toDomain(),
    amount: amount,
    status: status.toDomain(),
    payments: payments.map((p) => p.toDomain()).toList(),
    reminder: reminder?.toDomain(),
    nextReminderDate:
        nextReminderDate != null ? DateTime.tryParse(nextReminderDate!) : null,
    createdAt: createdAt != null ? DateTime.tryParse(createdAt!) : null,
    updatedAt: updatedAt != null ? DateTime.tryParse(updatedAt!) : null,
  );
}
