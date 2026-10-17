import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/payment_embedded.dart';

part 'payment_embedded_model.freezed.dart';
part 'payment_embedded_model.g.dart';

@freezed
sealed class PaymentEmbeddedModel with _$PaymentEmbeddedModel {
  const factory PaymentEmbeddedModel({
    required bool paid,
    String? paidTime,
    @JsonKey(fromJson: _amountFromJson) double? amountPaid,
  }) = _PaymentEmbeddedModel;

  factory PaymentEmbeddedModel.fromJson(Map<String, dynamic> json) =>
      _$PaymentEmbeddedModelFromJson(json);
}

// Backend BigDecimal: sayı ya da string gelebilir.
double? _amountFromJson(dynamic value) {
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value);
  return null;
}

extension PaymentEmbeddedModelMapper on PaymentEmbeddedModel {
  PaymentEmbedded toDomain() => PaymentEmbedded(
    paid: paid,
    paidTime: paidTime != null ? DateTime.tryParse(paidTime!) : null,
    amountPaid: amountPaid,
  );
}

extension PaymentEmbeddedDomainMapper on PaymentEmbedded {
  PaymentEmbeddedModel toModel() => PaymentEmbeddedModel(
    paid: paid,
    paidTime: paidTime?.toIso8601String().replaceAll(
      RegExp(r'(\.\d{3})\d+'),
      r'$1',
    ),
    amountPaid: amountPaid,
  );
}
