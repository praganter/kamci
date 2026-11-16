import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/debt/presentation/screens/debt_detail_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

const _owner = '6afaf2bb399846caf42061a0';
const _id = '6afaf2bb7c4cf716a14bd7d9';

FakeApi _api() => FakeApi()
    .on('GET /debt/$_id', fixture('debt_by_id'))
    .on('GET /category/getAll', fixture('category_get_all'))
    .on('GET /auth/user-detail', fixture('auth_user_detail'))
    .on('GET /debt/get', fixture('debt_get_grouped'))
    .on('POST /debt/pay/$_id', fixture('debt_pay_partial'))
    .on('POST /debt/skip/$_id', fixture('debt_pay_partial'))
    .on('POST /debt/$_id/set-paid-installments', fixture('debt_pay_partial'))
    .on('DELETE /debt/delete/$_id', null, 204);

Future<FakeApi> _open(WidgetTester t, {String userId = _owner, FakeApi? api}) async {
  final a = api ?? _api();
  await pumpScreen(t, const DebtDetailScreen(id: _id), api: a, userId: userId);
  await settle(t);
  return a;
}

Future<void> _openPayments(WidgetTester t) async {
  await t.tap(find.text('Payment Actions'));
  await settle(t);
}

List<RecordedRequest> _posts(FakeApi a, String path) =>
    a.requests.where((r) => r.method == 'POST' && r.path == path).toList();

void main() {
  testWidgets('shows the debt details from the backend', (t) async {
    await _open(t);
    expect(find.text('Rent'), findsOneWidget);
    expect(find.text('₺500'), findsOneWidget);
    expect(find.text('Active'), findsOneWidget);
    expect(find.text('Monthly'), findsOneWidget);
    expect(find.text('0 / 6'), findsOneWidget);
    expect(find.text('No payments yet'), findsOneWidget);
  });

  testWidgets('a missing debt surfaces the error state', (t) async {
    final api = FakeApi().on('GET /debt/$_id', {'message': 'x'}, 404);
    await _open(t, api: api);
    expect(find.text('Rent'), findsNothing);
    expect(find.byType(FilledButton), findsOneWidget);
  });

  testWidgets('Full Payment posts a payment without a body', (t) async {
    final api = await _open(t);
    await _openPayments(t);
    await t.tap(find.text('Full Payment'));
    await settle(t);
    final pay = _posts(api, '/debt/pay/$_id');
    expect(pay, hasLength(1));
    expect(pay.single.data, isNull);
  });

  testWidgets('Skip Payment posts to skip', (t) async {
    final api = await _open(t);
    await _openPayments(t);
    await t.tap(find.text('Skip Payment'));
    await settle(t);
    expect(_posts(api, '/debt/skip/$_id'), hasLength(1));
  });

  testWidgets('Partial Payment sends the typed amount; invalid input is rejected', (t) async {
    final api = await _open(t);
    await _openPayments(t);
    await t.tap(find.text('Partial Payment'));
    await settle(t);

    await t.enterText(find.byType(TextFormField), '0');
    await t.testTextInput.receiveAction(TextInputAction.done);
    await settle(t);
    expect(_posts(api, '/debt/pay/$_id'), isEmpty, reason: 'zero is not a valid amount');

    await t.enterText(find.byType(TextFormField), '125,5');
    await t.testTextInput.receiveAction(TextInputAction.done);
    await settle(t);
    expect(_posts(api, '/debt/pay/$_id').single.data, {'amountPaid': 125.5});
  });

  testWidgets('Bulk Payment is gated behind premium', (t) async {
    final api = await _open(t);
    await _openPayments(t);
    await t.tap(find.text('Bulk Payment'));
    await settle(t);
    expect(find.byType(AlertDialog), findsNothing);
    expect(_posts(api, '/debt/pay/$_id/bulk'), isEmpty);
  });

  testWidgets('set paid installments validates and posts the count', (t) async {
    final api = await _open(t);
    await t.tap(find.byIcon(Icons.playlist_add_check));
    await settle(t);

    await t.tap(find.text('Save'));
    await settle(t);
    expect(_posts(api, '/debt/$_id/set-paid-installments'), isEmpty);

    await t.enterText(find.byType(TextFormField), '2');
    await t.tap(find.text('Save'));
    await settle(t);
    expect(_posts(api, '/debt/$_id/set-paid-installments').single.data, {'count': 2});
  });

  testWidgets('edit opens the edit route', (t) async {
    await _open(t);
    await t.tap(find.byIcon(Icons.edit_outlined));
    await settle(t);
    expect(find.text('route:/debts/$_id/edit'), findsOneWidget);
  });

  testWidgets('delete needs confirmation, then removes and closes the screen', (t) async {
    final api = await _open(t);
    await t.tap(find.byIcon(Icons.delete_outline));
    await settle(t);
    expect(api.requests.where((r) => r.method == 'DELETE'), isEmpty);

    await t.tap(find.text('Delete'));
    await settle(t);
    expect(api.requests.where((r) => r.method == 'DELETE').single.path, '/debt/delete/$_id');
    expect(find.text('host'), findsOneWidget);
  });

  testWidgets('a read-only member cannot delete or pay', (t) async {
    final cats = [
      {
        'id': '6afaf2bb7c4cf716a14bd7d8',
        'name': 'Home',
        'ownerId': _owner,
        'members': [
          {'userId': 'viewer-1', 'email': 'v@example.com', 'accessLevel': 'READ'},
        ],
      },
    ];
    final api = _api().on('GET /category/getAll', cats);
    await _open(t, userId: 'viewer-1', api: api);

    await t.tap(find.byIcon(Icons.delete_outline));
    await settle(t);
    expect(find.text('Delete'), findsNothing, reason: 'no confirm dialog without delete rights');

    await _openPayments(t);
    expect(find.text('Full Payment'), findsNothing);
    expect(api.requests.where((r) => r.method != 'GET'), isEmpty);
  });
}
