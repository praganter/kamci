import 'dart:convert';
import 'dart:io';

import 'package:kamci/features/auth/data/models/auth_response.dart';
import 'package:kamci/features/category/data/models/category_model.dart';
import 'package:kamci/features/charts/data/models/monthly_summary_model.dart';
import 'package:kamci/features/debt/data/models/debt_model.dart';
import 'package:kamci/features/debt/data/models/debt_summary_model.dart';
import 'package:kamci/features/income/data/models/income_model.dart';
import 'package:flutter_test/flutter_test.dart';

dynamic _fixture(String name) =>
    jsonDecode(File('test/fixtures/$name.json').readAsStringSync());

void main() {
  group('Jackson 3 backend responses', () {
    test('auth: token + refreshToken', () {
      final r = AuthResponse.fromJson(_fixture('auth_refresh'));
      expect(r.accessToken, isNotEmpty);
      expect(r.refreshToken, isNotEmpty);
    });

    test('category list', () {
      final cats = (_fixture('category_get_all') as List)
          .map((e) => CategoryModel.fromJson(e).toDomain())
          .toList();
      expect(cats.single.name, 'Home');
      expect(cats.single.ownerId, isNotEmpty);
    });

    test('debt/get grouped summary', () {
      final raw = _fixture('debt_get_grouped')['categories'][0]['debts'][0];
      final debt = DebtSummaryModel.fromJson(raw).toDomain();
      expect(debt.nextDueDate, DateTime(2026, 12, 1, 10));
      expect(debt.amount, 500.0);
    });

    test('debt save/pay envelope: {"debt": {...}} with ISO dates', () {
      final json = _fixture('debt_pay_partial')['debt'] as Map<String, dynamic>;
      final debt = DebtModel.fromJson(json).toDomain();
      expect(debt.paymentStatus.name, 'partiallyPaid');
      expect(debt.firstPaymentDate.hour, 10);
      expect(debt.createdAt, isNotNull);
      expect(json['updatedDate'], matches(RegExp(r'\.\d{7,9}$')));
      expect(debt.updatedAt, isNotNull);
    });

    test('debt by id (bare object)', () {
      final debt = DebtModel.fromJson(_fixture('debt_by_id')).toDomain();
      expect(debt.name, 'Rent');
      expect(debt.installmentCount, 6);
    });

    test('income current + history', () {
      final cur = IncomeModel.fromJson(_fixture('income_current')).toDomain();
      expect(cur.amount, 10000.0);
      final hist = _fixture('income_history') as List;
      expect(IncomeModel.fromJson(hist.first).toDomain().currency, 'TRY');
    });

    test('debt/summary: month, trend and paidTime parse', () {
      final s = MonthlySummaryModel.fromJson(_fixture('debt_summary')).toDomain();
      expect(s.totalPaid, 100.0);
      expect(s.paidCount, 1);
      expect(s.trend, isNotEmpty);
    });
  });
}
