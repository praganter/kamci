import 'package:dt_mobile/features/debt/data/models/debt_summary_model.dart';
import 'package:dt_mobile/features/debt/data/models/payment_embedded_model.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> _summaryJson([Map<String, dynamic> extra = const {}]) => {
  'id': 'd1',
  'name': 'Kira',
  'categoryId': 'c1',
  'paymentPeriodType': 'MONTHLY',
  'amount': 500,
  'currency': 'TRY',
  ...extra,
};

void main() {
  group('DebtSummaryModel.nextDueDate (debt/get)', () {
    test('backend ISO LocalDateTime domain modele taşınır', () {
      final debt = DebtSummaryModel.fromJson(
        _summaryJson({'nextDueDate': '2026-10-20T00:00:00'}),
      ).toDomain();
      expect(debt.nextDueDate, DateTime(2026, 10, 20));
    });

    test('alan yok ya da null → null (eski backend)', () {
      expect(
        DebtSummaryModel.fromJson(_summaryJson()).toDomain().nextDueDate,
        isNull,
      );
      expect(
        DebtSummaryModel.fromJson(
          _summaryJson({'nextDueDate': null}),
        ).toDomain().nextDueDate,
        isNull,
      );
    });
  });

  group('PaymentEmbeddedModel.amountPaid', () {
    test('sayı ve BigDecimal-string olarak parse edilir', () {
      expect(
        PaymentEmbeddedModel.fromJson({
          'paid': true,
          'amountPaid': 250.5,
        }).toDomain().amountPaid,
        250.5,
      );
      expect(
        PaymentEmbeddedModel.fromJson({
          'paid': true,
          'amountPaid': '1200.00',
        }).toDomain().amountPaid,
        1200.0,
      );
    });

    test('alan yoksa null', () {
      expect(
        PaymentEmbeddedModel.fromJson({'paid': false}).toDomain().amountPaid,
        isNull,
      );
    });
  });
}
