import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:in_app_purchase/in_app_purchase.dart';
import 'package:kamci/features/subscription/data/store/purchase_store.dart';
import 'package:kamci/features/subscription/presentation/screens/premium_screen.dart';
import 'package:kamci/features/subscription/subscription_constants.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/fake_store.dart';
import '../../helpers/fakes.dart';
import '../../helpers/pump_app.dart';

const _storeError =
    "Couldn't connect to the store. Check your internet connection.";

final _verified = {
  'plan': 'YEARLY',
  'status': 'ACTIVE',
  'expiryDate': '2027-01-01T00:00:00',
  'accessToken': fakeJwt(premium: true),
  'refreshToken': 'new-refresh',
};

Future<(Harness, FakePurchaseStore)> _pump(
  WidgetTester t, {
  FakePurchaseStore? store,
  FakeApi? api,
}) async {
  final s = store ?? FakePurchaseStore();
  final h = await pumpScreen(
    t,
    const PremiumScreen(),
    api: api ?? FakeApi(),
    overrides: [purchaseStoreProvider.overrideWithValue(s)],
  );
  await settle(t);
  return (h, s);
}

void main() {
  testWidgets('lists the premium benefits', (t) async {
    await _pump(t);
    expect(find.text('Unlimited debts'), findsOneWidget);
    expect(find.text('Unlimited categories'), findsOneWidget);
    expect(find.text('Debt reminders & notifications'), findsOneWidget);
    expect(find.text('Category sharing with friends'), findsOneWidget);
  });

  testWidgets('without a store connection it explains and offers retry', (
    t,
  ) async {
    await _pump(t, store: FakePurchaseStore(available: false));
    expect(find.text(_storeError), findsOneWidget);
    await t.tap(find.text('Retry'));
    await settle(t);
    expect(find.text(_storeError), findsOneWidget);
  });

  testWidgets('the close button leaves the screen', (t) async {
    await _pump(t);
    await t.tap(find.byType(IconButton).first);
    await settle(t);
    expect(find.text('host'), findsOneWidget);
  });

  testWidgets('shows the store plans with prices and a best value badge', (
    t,
  ) async {
    await _pump(t);
    expect(find.text('Yearly'), findsOneWidget);
    expect(find.text('Monthly'), findsOneWidget);
    expect(find.text(r'$59.99 / year'), findsOneWidget);
    expect(find.text(r'$5.99 / month'), findsOneWidget);
    expect(find.text('Best Value'), findsOneWidget);
    expect(find.text(_storeError), findsNothing);
  });

  testWidgets('an unavailable store can be retried once it is back', (t) async {
    final store = FakePurchaseStore(available: false);
    await _pump(t, store: store);
    expect(find.text(_storeError), findsOneWidget);
    store.available = true;
    await t.tap(find.text('Retry'));
    await settle(t);
    expect(find.text(_storeError), findsNothing);
    expect(find.text('Yearly'), findsOneWidget);
  });

  testWidgets('a store without our products says the plans could not load', (
    t,
  ) async {
    await _pump(t, store: FakePurchaseStore(products: const []));
    expect(
      find.text("Subscription plans couldn't be loaded right now."),
      findsOneWidget,
    );
  });

  testWidgets('subscribing buys the preselected yearly plan', (t) async {
    final (_, store) = await _pump(t);
    await t.tap(find.text('Get Premium'));
    await settle(t);
    expect(store.bought, [SubscriptionConstants.yearlyProductId]);
  });

  testWidgets('picking the monthly plan buys the monthly product', (t) async {
    final (_, store) = await _pump(t);
    await t.tap(find.text('Monthly'));
    await t.pump();
    await t.tap(find.text('Get Premium'));
    await settle(t);
    expect(store.bought, [SubscriptionConstants.monthlyProductId]);
  });

  testWidgets('a verified purchase closes the screen', (t) async {
    final (_, store) = await _pump(
      t,
      api: FakeApi().on('POST /subscription/verify', _verified),
    );
    await t.tap(find.text('Get Premium'));
    await settle(t);
    store.emit(
      fakePurchase(
        SubscriptionConstants.yearlyProductId,
        PurchaseStatus.purchased,
      ),
    );
    await settle(t);
    expect(find.text('host'), findsOneWidget);
  });

  testWidgets('a rejected receipt shows the localized error and stays', (
    t,
  ) async {
    final (_, store) = await _pump(
      t,
      api: FakeApi().on('POST /subscription/verify', {
        'code': 'error.subscription.verification_failed',
        'message': 'bad receipt',
      }, 400),
    );
    store.emit(
      fakePurchase(
        SubscriptionConstants.yearlyProductId,
        PurchaseStatus.purchased,
      ),
    );
    await settle(t);
    expect(find.text('Subscription verification failed.'), findsOneWidget);
    expect(find.text('Get Premium'), findsOneWidget);
  });

  testWidgets('a store error shows the generic purchase failure', (t) async {
    final (_, store) = await _pump(t);
    store.emit(
      fakePurchase(
        SubscriptionConstants.yearlyProductId,
        PurchaseStatus.error,
        error: IAPError(source: 'test', code: 'x', message: 'raw store text'),
      ),
    );
    await settle(t);
    expect(
      find.text('Something went wrong during the purchase.'),
      findsOneWidget,
    );
    expect(find.text('raw store text'), findsNothing);
  });

  testWidgets('a pending purchase tells the user it awaits approval', (
    t,
  ) async {
    final (_, store) = await _pump(t);
    store.emit(
      fakePurchase(
        SubscriptionConstants.yearlyProductId,
        PurchaseStatus.pending,
      ),
    );
    await settle(t);
    expect(find.textContaining('waiting for approval'), findsOneWidget);
  });

  testWidgets('a cancelled purchase is silent and re-enables the button', (
    t,
  ) async {
    final (_, store) = await _pump(t);
    await t.tap(find.text('Get Premium'));
    await settle(t);
    store.emit(
      fakePurchase(
        SubscriptionConstants.yearlyProductId,
        PurchaseStatus.canceled,
      ),
    );
    await settle(t);
    expect(find.byType(SnackBar), findsNothing);
    expect(find.text('host'), findsNothing);
  });

  testWidgets('restoring with no previous purchases says so', (t) async {
    final (_, store) = await _pump(t);
    await t.tap(find.text('Restore Purchases'));
    await t.pump();
    expect(store.restores, 1);
    await t.pump(const Duration(seconds: 11));
    expect(
      find.text('No previous purchases were found to restore.'),
      findsOneWidget,
    );
  });
}
