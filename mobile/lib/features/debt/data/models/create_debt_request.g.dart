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
      startDate: json['startDate'] as String,
      endDate: json['endDate'] as String?,
      paymentPeriodType: $enumDecode(
        _$PaymentPeriodTypeJsonEnumMap,
        json['paymentPeriodType'],
      ),
      amount: (json['amount'] as num).toDouble(),
      status:
          $enumDecodeNullable(_$DebtStatusJsonEnumMap, json['status']) ??
          DebtStatusJson.active,
    );

Map<String, dynamic> _$CreateDebtRequestToJson(_CreateDebtRequest instance) =>
    <String, dynamic>{
      if (instance.id case final value?) 'id': value,
      'name': instance.name,
      if (instance.categoryId case final value?) 'categoryId': value,
      if (instance.description case final value?) 'description': value,
      'startDate': instance.startDate,
      if (instance.endDate case final value?) 'endDate': value,
      'paymentPeriodType':
          _$PaymentPeriodTypeJsonEnumMap[instance.paymentPeriodType]!,
      'amount': instance.amount,
      'status': _$DebtStatusJsonEnumMap[instance.status]!,
    };

const _$PaymentPeriodTypeJsonEnumMap = {
  PaymentPeriodTypeJson.daily: 'DAILY',
  PaymentPeriodTypeJson.weekly: 'WEEKLY',
  PaymentPeriodTypeJson.monthly: 'MONTHLY',
  PaymentPeriodTypeJson.quarterly: 'QUARTERLY',
  PaymentPeriodTypeJson.yearly: 'YEARLY',
};

const _$DebtStatusJsonEnumMap = {
  DebtStatusJson.active: 'ACTIVE',
  DebtStatusJson.inactive: 'INACTIVE',
  DebtStatusJson.completed: 'COMPLETED',
};
