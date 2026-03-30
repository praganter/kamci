// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'subscription_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_SubscriptionModel _$SubscriptionModelFromJson(Map<String, dynamic> json) =>
    _SubscriptionModel(
      plan: json['plan'] as String,
      status: json['status'] as String,
      expiresDate: json['expiresDate'] == null
          ? null
          : DateTime.parse(json['expiresDate'] as String),
    );

Map<String, dynamic> _$SubscriptionModelToJson(_SubscriptionModel instance) =>
    <String, dynamic>{
      'plan': instance.plan,
      'status': instance.status,
      if (instance.expiresDate?.toIso8601String() case final value?)
        'expiresDate': value,
    };
