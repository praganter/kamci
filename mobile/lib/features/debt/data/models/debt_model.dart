import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/debt.dart';
import 'payment_embedded_model.dart';
import 'reminder_embedded_model.dart';

part 'debt_model.freezed.dart';
part 'debt_model.g.dart';

@JsonEnum(alwaysCreate: true)
enum PaymentPeriodTypeJson {
  @JsonValue('ONE_TIME')
  oneTime,
  @JsonValue('DAILY')
  daily,
  @JsonValue('WEEKLY')
  weekly,
  @JsonValue('BI_WEEKLY')
  biWeekly,
  @JsonValue('MONTHLY')
  monthly,
  @JsonValue('QUARTERLY')
  quarterly,
  @JsonValue('HALF_YEARLY')
  halfYearly,
  @JsonValue('YEARLY')
  yearly;

  PaymentPeriodType toDomain() => switch (this) {
    PaymentPeriodTypeJson.oneTime => PaymentPeriodType.oneTime,
    PaymentPeriodTypeJson.daily => PaymentPeriodType.daily,
    PaymentPeriodTypeJson.weekly => PaymentPeriodType.weekly,
    PaymentPeriodTypeJson.biWeekly => PaymentPeriodType.biWeekly,
    PaymentPeriodTypeJson.monthly => PaymentPeriodType.monthly,
    PaymentPeriodTypeJson.quarterly => PaymentPeriodType.quarterly,
    PaymentPeriodTypeJson.halfYearly => PaymentPeriodType.halfYearly,
    PaymentPeriodTypeJson.yearly => PaymentPeriodType.yearly,
  };

  static PaymentPeriodTypeJson fromDomain(PaymentPeriodType t) => switch (t) {
    PaymentPeriodType.oneTime => PaymentPeriodTypeJson.oneTime,
    PaymentPeriodType.daily => PaymentPeriodTypeJson.daily,
    PaymentPeriodType.weekly => PaymentPeriodTypeJson.weekly,
    PaymentPeriodType.biWeekly => PaymentPeriodTypeJson.biWeekly,
    PaymentPeriodType.monthly => PaymentPeriodTypeJson.monthly,
    PaymentPeriodType.quarterly => PaymentPeriodTypeJson.quarterly,
    PaymentPeriodType.halfYearly => PaymentPeriodTypeJson.halfYearly,
    PaymentPeriodType.yearly => PaymentPeriodTypeJson.yearly,
  };
}

@JsonEnum(alwaysCreate: true)
enum DebtStatusJson {
  @JsonValue('ACTIVE')
  active,
  @JsonValue('COMPLETED')
  completed;

  DebtStatus toDomain() => switch (this) {
    DebtStatusJson.active => DebtStatus.active,
    DebtStatusJson.completed => DebtStatus.completed,
  };

  static DebtStatusJson fromDomain(DebtStatus s) => switch (s) {
    DebtStatus.active => DebtStatusJson.active,
    DebtStatus.completed => DebtStatusJson.completed,
  };
}

@JsonEnum(alwaysCreate: true)
enum PaymentStatusJson {
  @JsonValue('PAID')
  paid,
  @JsonValue('UNPAID')
  unpaid,
  @JsonValue('OVERDUE')
  overdue,
  @JsonValue('PARTIALLY_PAID')
  partiallyPaid;

  PaymentStatus toDomain() => switch (this) {
    PaymentStatusJson.paid => PaymentStatus.paid,
    PaymentStatusJson.unpaid => PaymentStatus.unpaid,
    PaymentStatusJson.overdue => PaymentStatus.overdue,
    PaymentStatusJson.partiallyPaid => PaymentStatus.partiallyPaid,
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
    String? firstPaymentDate,
    int? installmentCount,
    int? paidInstallmentCount,
    required PaymentPeriodTypeJson paymentPeriodType,
    @JsonKey(fromJson: _amountFromJson) required double amount,
    @Default(DebtStatusJson.active) DebtStatusJson status,
    @Default(PaymentStatusJson.unpaid) PaymentStatusJson paymentStatus,
    String? currency,
    @Default(false) bool autoPayment,
    @Default([]) List<PaymentEmbeddedModel> payments,
    ReminderEmbeddedModel? reminder,
    String? nextReminderDate,
    String? nextAutoPaymentDate,
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
    firstPaymentDate: firstPaymentDate != null
        ? DateTime.parse(firstPaymentDate!)
        : DateTime(2000),
    installmentCount: installmentCount,
    paidInstallmentCount: paidInstallmentCount,
    paymentPeriodType: paymentPeriodType.toDomain(),
    amount: amount,
    currency: currency,
    status: status.toDomain(),
    autoPayment: autoPayment,
    paymentStatus: paymentStatus.toDomain(),
    payments: payments.map((p) => p.toDomain()).toList(),
    reminder: reminder?.toDomain(),
    nextReminderDate: nextReminderDate != null
        ? DateTime.tryParse(nextReminderDate!)
        : null,
    createdAt: createdAt != null ? DateTime.tryParse(createdAt!) : null,
    updatedAt: updatedAt != null ? DateTime.tryParse(updatedAt!) : null,
  );
}
