import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/subscription/presentation/screens/premium_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

const _storeError =
    "Couldn't connect to the store. Check your internet connection.";

void main() {
  testWidgets('lists the premium benefits', (t) async {
    await pumpScreen(t, const PremiumScreen(), api: FakeApi());
    await settle(t);
    expect(find.text('Unlimited debts'), findsOneWidget);
    expect(find.text('Unlimited categories'), findsOneWidget);
    expect(find.text('Debt reminders & notifications'), findsOneWidget);
    expect(find.text('Category sharing with friends'), findsOneWidget);
  });

  testWidgets('without a store connection it explains and offers retry', (
    t,
  ) async {
    await pumpScreen(t, const PremiumScreen(), api: FakeApi());
    await settle(t);
    expect(find.text(_storeError), findsOneWidget);
    await t.tap(find.text('Retry'));
    await settle(t);
    expect(find.text(_storeError), findsOneWidget);
  });

  testWidgets('the close button leaves the screen', (t) async {
    await pumpScreen(t, const PremiumScreen(), api: FakeApi());
    await settle(t);
    await t.tap(find.byType(IconButton).first);
    await settle(t);
    expect(find.text('host'), findsOneWidget);
  });
}
