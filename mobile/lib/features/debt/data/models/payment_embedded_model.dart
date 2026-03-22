import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/payment_embedded.dart';

part 'payment_embedded_model.freezed.dart';
part 'payment_embedded_model.g.dart';

@freezed
abstract class PaymentEmbeddedModel with _$PaymentEmbeddedModel {
  const factory PaymentEmbeddedModel({
    required bool paid,
    String? paidTime,
  }) = _PaymentEmbeddedModel;

  factory PaymentEmbeddedModel.fromJson(Map<String, dynamic> json) =>
      _$PaymentEmbeddedModelFromJson(json);
}

extension PaymentEmbeddedModelMapper on PaymentEmbeddedModel {
  PaymentEmbedded toDomain() => PaymentEmbedded(
    paid: paid,
    paidTime: paidTime != null ? DateTime.tryParse(paidTime!) : null,
  );
}

extension PaymentEmbeddedDomainMapper on PaymentEmbedded {
  PaymentEmbeddedModel toModel() => PaymentEmbeddedModel(
    paid: paid,
    paidTime: paidTime?.toIso8601String().replaceAll(RegExp(r'(\.\d{3})\d+'), r'$1'),
  );
}
