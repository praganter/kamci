import 'package:freezed_annotation/freezed_annotation.dart';
import 'debt_model.dart';
import 'reminder_embedded_model.dart';

part 'create_debt_request.freezed.dart';
part 'create_debt_request.g.dart';

@freezed
abstract class CreateDebtRequest with _$CreateDebtRequest {
  const factory CreateDebtRequest({
    String? id,
    required String name,
    String? categoryId,
    String? description,
    required String firstPaymentDate,
    int? installmentCount,
    required PaymentPeriodTypeJson paymentPeriodType,
    required double amount,
    @Default(false) bool autoPayment,
    String? currency,
    ReminderEmbeddedModel? reminder,
  }) = _CreateDebtRequest;

  factory CreateDebtRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateDebtRequestFromJson(json);
}
