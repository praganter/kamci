import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/debt_summary.dart';
import 'debt_model.dart';

part 'debt_summary_model.freezed.dart';
part 'debt_summary_model.g.dart';

@freezed
sealed class DebtSummaryModel with _$DebtSummaryModel {
  const factory DebtSummaryModel({
    required String id,
    required String name,
    required String categoryId,
    int? installmentCount,
    @Default(0) int paidCount,
    required PaymentPeriodTypeJson paymentPeriodType,
    @JsonKey(fromJson: _summaryAmountFromJson) required double amount,
    required String currency,
    @Default(DebtStatusJson.active) DebtStatusJson status,
    @Default(PaymentStatusJson.unpaid) PaymentStatusJson paymentStatus,
    @Default(false) bool autoPayment,
    @Default(false) bool paidCurrentPeriod,
    @Default(false) bool hasReminder,
  }) = _DebtSummaryModel;

  factory DebtSummaryModel.fromJson(Map<String, dynamic> json) =>
      _$DebtSummaryModelFromJson(json);
}

double _summaryAmountFromJson(dynamic value) {
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0.0;
  return 0.0;
}

extension DebtSummaryModelMapper on DebtSummaryModel {
  DebtSummary toDomain() => DebtSummary(
    id: id,
    name: name,
    categoryId: categoryId,
    installmentCount: installmentCount,
    paidCount: paidCount,
    paymentPeriodType: paymentPeriodType.toDomain(),
    amount: amount,
    currency: currency,
    status: status.toDomain(),
    paymentStatus: paymentStatus.toDomain(),
    autoPayment: autoPayment,
    paidCurrentPeriod: paidCurrentPeriod,
    hasReminder: hasReminder,
  );
}
