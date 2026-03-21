// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'debt_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_DebtModel _$DebtModelFromJson(Map<String, dynamic> json) => _DebtModel(
  id: json['id'] as String,
  name: json['name'] as String,
  ownerId: json['ownerId'] as String,
  categoryId: json['categoryId'] as String?,
  description: json['description'] as String?,
  startDate: json['startDate'] as String,
  endDate: json['endDate'] as String?,
  paymentPeriodType: $enumDecode(
    _$PaymentPeriodTypeJsonEnumMap,
    json['paymentPeriodType'],
  ),
  amount: _amountFromJson(json['amount']),
  status:
      $enumDecodeNullable(_$DebtStatusJsonEnumMap, json['status']) ??
      DebtStatusJson.active,
  payments:
      (json['payments'] as List<dynamic>?)
          ?.map((e) => PaymentEmbeddedModel.fromJson(e as Map<String, dynamic>))
          .toList() ??
      const [],
  reminder: json['reminder'] == null
      ? null
      : ReminderEmbeddedModel.fromJson(
          json['reminder'] as Map<String, dynamic>,
        ),
  nextReminderDate: json['nextReminderDate'] as String?,
  createdAt: json['createdDate'] as String?,
  updatedAt: json['updatedDate'] as String?,
);

Map<String, dynamic> _$DebtModelToJson(
  _DebtModel instance,
) => <String, dynamic>{
  'id': instance.id,
  'name': instance.name,
  'ownerId': instance.ownerId,
  if (instance.categoryId case final value?) 'categoryId': value,
  if (instance.description case final value?) 'description': value,
  'startDate': instance.startDate,
  if (instance.endDate case final value?) 'endDate': value,
  'paymentPeriodType':
      _$PaymentPeriodTypeJsonEnumMap[instance.paymentPeriodType]!,
  'amount': instance.amount,
  'status': _$DebtStatusJsonEnumMap[instance.status]!,
  'payments': instance.payments.map((e) => e.toJson()).toList(),
  if (instance.reminder?.toJson() case final value?) 'reminder': value,
  if (instance.nextReminderDate case final value?) 'nextReminderDate': value,
  if (instance.createdAt case final value?) 'createdDate': value,
  if (instance.updatedAt case final value?) 'updatedDate': value,
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
