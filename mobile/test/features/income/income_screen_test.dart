import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/core/network/dio_client.dart';
import 'package:kamci/features/income/presentation/screens/income_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

FakeApi _api() => FakeApi()
    .on('GET /income/history', fixture('income_history'))
    .on('GET /income/current', fixture('income_current'));

void main() {
  testWidgets('lists the income history', (tester) async {
    await pumpScreen(tester, const IncomeScreen(), api: _api());
    await settle(tester);
    expect(find.textContaining('10,000.00'), findsWidgets);
    expect(find.text('01 Nov 2026'), findsWidgets);
  });

  testWidgets('empty history shows the empty state', (tester) async {
    final api = FakeApi().on('GET /income/history', <Object>[]);
    final h = await pumpScreen(tester, const IncomeScreen(), api: api);
    await settle(tester);
    expect(find.text(h.l10n.noIncomeYet), findsOneWidget);
  });

  testWidgets('a failed load shows the error widget and retry refetches', (tester) async {
    final api = FakeApi().on('GET /income/history', {'message': 'x'}, 500);
    await pumpScreen(tester, const IncomeScreen(), api: api);
    await settle(tester);
    final before = api.requests.length;
    await tester.tap(find.byType(FilledButton));
    await settle(tester);
    expect(api.requests.length, greaterThan(before));
  });

  testWidgets('swipe to delete asks for confirmation, then deletes', (tester) async {
    final id = (fixture('income_history') as List).first['id'] as String;
    final history = List<Object?>.of(fixture('income_history') as List);
    final api = FakeApi()
        .on('GET /income/history', () => history)
        .on('GET /income/current', fixture('income_current'))
        .on('DELETE /income/delete/$id', () {
          history.removeAt(0);
          return null;
        }, 204);
    final h = await pumpScreen(tester, const IncomeScreen(), api: api);
    await settle(tester);

    await tester.drag(find.byType(Dismissible).first, const Offset(-600, 0));
    await settle(tester);
    expect(find.text(h.l10n.deleteIncomeConfirm), findsOneWidget);
    expect(api.requests.where((r) => r.method == 'DELETE'), isEmpty);

    await tester.tap(find.text(h.l10n.delete).last);
    await settle(tester);
    expect(api.requests.where((r) => r.method == 'DELETE').single.path, '/income/delete/$id');
    expect(find.byType(Dismissible), findsNothing);
    expect(find.text(h.l10n.noIncomeYet), findsOneWidget);
  });

  testWidgets('the row disappears before the delete call finishes', (tester) async {
    final id = (fixture('income_history') as List).first['id'] as String;
    final gate = Completer<void>();
    final api = _api();
    final dio = api.dio();
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, handler) async {
          if (o.method == 'DELETE') await gate.future;
          handler.next(o);
        },
      ),
    );
    api.on('DELETE /income/delete/$id', null, 204);
    final h = await pumpScreen(
      tester,
      const IncomeScreen(),
      api: api,
      overrides: [dioClientProvider.overrideWithValue(dio)],
    );
    await settle(tester);

    await tester.drag(find.byType(Dismissible).first, const Offset(-600, 0));
    await settle(tester);
    await tester.tap(find.text(h.l10n.delete).last);
    await settle(tester);

    expect(find.byType(Dismissible), findsNothing, reason: 'removed optimistically');
    gate.complete();
    await settle(tester);
  });

  testWidgets('a failed delete puts the row back and reports the error', (tester) async {
    final id = (fixture('income_history') as List).first['id'] as String;
    final api = _api().on('DELETE /income/delete/$id', {'message': 'boom'}, 500);
    final h = await pumpScreen(tester, const IncomeScreen(), api: api);
    await settle(tester);

    await tester.drag(find.byType(Dismissible).first, const Offset(-600, 0));
    await settle(tester);
    await tester.tap(find.text(h.l10n.delete).last);
    await settle(tester);

    await tester.pump(const Duration(seconds: 1));
    expect(find.byKey(ValueKey(id)), findsOneWidget);
    expect(find.byType(SnackBar), findsOneWidget);
  });

  testWidgets('add sheet validates before saving', (tester) async {
    final api = _api();
    final h = await pumpScreen(tester, const IncomeScreen(), api: api);
    await settle(tester);

    await tester.tap(find.byType(FloatingActionButton));
    await settle(tester);
    expect(find.text(h.l10n.addIncome), findsWidgets);
    await tester.tap(find.text(h.l10n.save));
    await settle(tester);

    expect(find.text(h.l10n.validationAmountRequired), findsOneWidget);
    expect(api.requests.where((r) => r.method == 'POST'), isEmpty);
  });
}
