import 'package:freezed_annotation/freezed_annotation.dart';

part 'save_income_request.freezed.dart';
part 'save_income_request.g.dart';

@freezed
abstract class SaveIncomeRequest with _$SaveIncomeRequest {
  const factory SaveIncomeRequest({
    required double amount,
    required String currency,
    required String effectiveFrom,
    String? note,
  }) = _SaveIncomeRequest;

  factory SaveIncomeRequest.fromJson(Map<String, dynamic> json) =>
      _$SaveIncomeRequestFromJson(json);
}
