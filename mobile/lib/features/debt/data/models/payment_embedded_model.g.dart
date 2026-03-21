// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'payment_embedded_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_PaymentEmbeddedModel _$PaymentEmbeddedModelFromJson(
  Map<String, dynamic> json,
) => _PaymentEmbeddedModel(
  paid: json['paid'] as bool,
  paidTime: json['paidTime'] as String?,
);

Map<String, dynamic> _$PaymentEmbeddedModelToJson(
  _PaymentEmbeddedModel instance,
) => <String, dynamic>{
  'paid': instance.paid,
  if (instance.paidTime case final value?) 'paidTime': value,
};
