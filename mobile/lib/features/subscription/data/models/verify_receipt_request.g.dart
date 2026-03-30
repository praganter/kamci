// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'verify_receipt_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_VerifyReceiptRequest _$VerifyReceiptRequestFromJson(
  Map<String, dynamic> json,
) => _VerifyReceiptRequest(
  platform: json['platform'] as String,
  plan: json['plan'] as String,
  receiptData: json['receiptData'] as String?,
  purchaseToken: json['purchaseToken'] as String?,
);

Map<String, dynamic> _$VerifyReceiptRequestToJson(
  _VerifyReceiptRequest instance,
) => <String, dynamic>{
  'platform': instance.platform,
  'plan': instance.plan,
  if (instance.receiptData case final value?) 'receiptData': value,
  if (instance.purchaseToken case final value?) 'purchaseToken': value,
};
