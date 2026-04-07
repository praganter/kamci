import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entities/income.dart';

part 'income_model.freezed.dart';
part 'income_model.g.dart';

@freezed
abstract class IncomeModel with _$IncomeModel {
  const factory IncomeModel({
    required String id,
    @JsonKey(fromJson: _amountFromJson) required double amount,
    required String currency,
    required String effectiveFrom,
    String? note,
    @JsonKey(name: 'createdDate') String? createdAt,
  }) = _IncomeModel;

  factory IncomeModel.fromJson(Map<String, dynamic> json) =>
      _$IncomeModelFromJson(json);
}

double _amountFromJson(dynamic value) {
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0.0;
  return 0.0;
}

extension IncomeModelMapper on IncomeModel {
  Income toDomain() => Income(
    id: id,
    amount: amount,
    currency: currency,
    effectiveFrom: DateTime.parse(effectiveFrom),
    note: note,
    createdAt: createdAt != null ? DateTime.tryParse(createdAt!) : null,
  );
}
