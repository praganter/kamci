// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'create_debt_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_CreateDebtRequest _$CreateDebtRequestFromJson(Map<String, dynamic> json) =>
    _CreateDebtRequest(
      id: json['id'] as String?,
      name: json['name'] as String,
      categoryId: json['categoryId'] as String?,
      description: json['description'] as String?,
      firstPaymentDate: json['firstPaymentDate'] as String,
      lastPaymentDate: json['lastPaymentDate'] as String?,
      paymentPeriodType: $enumDecode(
        _$PaymentPeriodTypeJsonEnumMap,
        json['paymentPeriodType'],
      ),
      amount: (json['amount'] as num).toDouble(),
      autoPayment: json['autoPayment'] as bool? ?? false,
    );

Map<String, dynamic> _$CreateDebtRequestToJson(_CreateDebtRequest instance) =>
    <String, dynamic>{
      if (instance.id case final value?) 'id': value,
      'name': instance.name,
      if (instance.categoryId case final value?) 'categoryId': value,
      if (instance.description case final value?) 'description': value,
      'firstPaymentDate': instance.firstPaymentDate,
      if (instance.lastPaymentDate case final value?) 'lastPaymentDate': value,
      'paymentPeriodType':
          _$PaymentPeriodTypeJsonEnumMap[instance.paymentPeriodType]!,
      'amount': instance.amount,
      'autoPayment': instance.autoPayment,
    };

const _$PaymentPeriodTypeJsonEnumMap = {
  PaymentPeriodTypeJson.daily: 'DAILY',
  PaymentPeriodTypeJson.weekly: 'WEEKLY',
  PaymentPeriodTypeJson.monthly: 'MONTHLY',
  PaymentPeriodTypeJson.quarterly: 'QUARTERLY',
  PaymentPeriodTypeJson.yearly: 'YEARLY',
};
