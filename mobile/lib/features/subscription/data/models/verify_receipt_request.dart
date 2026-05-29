import 'package:freezed_annotation/freezed_annotation.dart';

part 'verify_receipt_request.freezed.dart';
part 'verify_receipt_request.g.dart';

@freezed
sealed class VerifyReceiptRequest with _$VerifyReceiptRequest {
  const factory VerifyReceiptRequest({
    required String receipt,
    required String platform,
  }) = _VerifyReceiptRequest;

  factory VerifyReceiptRequest.fromJson(Map<String, dynamic> json) =>
      _$VerifyReceiptRequestFromJson(json);
}
