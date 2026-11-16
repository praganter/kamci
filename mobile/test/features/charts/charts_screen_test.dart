import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/charts/presentation/screens/charts_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

Map<String, Object?> _summary([Map<String, Object?> patch = const {}]) =>
    {...(fixture('debt_summary') as Map<String, dynamic>), ...patch};

FakeApi _api(Object? Function() summary) =>
    FakeApi().on('GET /debt/summary', summary);

String _month(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}';

void main() {
  testWidgets('renders totals, breakdown and progress from the summary', (t) async {
    final h = await pumpScreen(t, const ChartsScreen(), api: _api(() => _summary()));
    await settle(t);

    expect(find.text('100 TRY'), findsWidgets);
    expect(find.text(h.l10n.noIncomeSet), findsOneWidget);
    expect(find.text('Rent'), findsOneWidget);
    expect(find.text('Home'), findsOneWidget);
    expect(find.text('17%'), findsOneWidget);
    expect(find.text('1 / 6 installments'), findsOneWidget);
  });

  testWidgets('shows the debt-to-income ratio when income is known', (t) async {
    await pumpScreen(
      t,
      const ChartsScreen(),
      api: _api(() => _summary({'incomeAtMonth': 1000.0, 'debtToIncomeRatio': 10.0})),
    );
    await settle(t);
    expect(find.text('10.0%'), findsOneWidget);
  });

  testWidgets('household mode shows the joint income and ratio rows', (t) async {
    final h = await pumpScreen(
      t,
      const ChartsScreen(),
      api: _api(
        () => _summary({
          'household': true,
          'householdIncomeAtMonth': 3000.0,
          'householdDebtToIncomeRatio': 3.3,
        }),
      ),
    );
    await settle(t);
    expect(find.text(h.l10n.householdIncomeLabel), findsOneWidget);
    expect(find.text(h.l10n.householdRatioLabel), findsOneWidget);
  });

  testWidgets('next-month is disabled on the current month; previous refetches', (t) async {
    final api = _api(() => _summary());
    await pumpScreen(t, const ChartsScreen(), api: api);
    await settle(t);

    final next = t.widget<IconButton>(find.widgetWithIcon(IconButton, Icons.chevron_right));
    expect(next.onPressed, isNull);

    await t.tap(find.byIcon(Icons.chevron_left));
    await settle(t);
    final now = DateTime.now();
    expect(api.requests.last.query['month'], _month(DateTime(now.year, now.month - 1)));

    final nextNow = t.widget<IconButton>(find.widgetWithIcon(IconButton, Icons.chevron_right));
    expect(nextNow.onPressed, isNotNull);
    await t.tap(find.byIcon(Icons.chevron_right));
    await settle(t);
    expect(api.requests.last.query['month'], _month(now));
  });

  testWidgets('an empty month shows the no-data message', (t) async {
    final h = await pumpScreen(
      t,
      const ChartsScreen(),
      api: _api(
        () => _summary({
          'totalPaid': 0,
          'paidCount': 0,
          'debts': <Object>[],
          'trend': <Object>[],
          'categoryBreakdown': <Object>[],
          'debtProgress': <Object>[],
        }),
      ),
    );
    await settle(t);
    expect(find.text(h.l10n.noDataForMonth), findsOneWidget);
  });

  testWidgets('a failed request shows an error with retry', (t) async {
    final api = FakeApi().on('GET /debt/summary', {'message': 'x'}, 500);
    await pumpScreen(t, const ChartsScreen(), api: api);
    await settle(t);
    final before = api.requests.length;
    await t.tap(find.byType(FilledButton));
    await settle(t);
    expect(api.requests.length, greaterThan(before));
  });
}
