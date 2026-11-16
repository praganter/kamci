import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/debt/presentation/screens/debt_list_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

FakeApi _api() => FakeApi()
    .on('GET /debt/get', fixture('debt_get_grouped'))
    .on('GET /category/getAll', fixture('category_get_all'))
    .on('GET /auth/user-detail', fixture('auth_user_detail'))
    .on('DELETE /category/delete/6afaf2bb7c4cf716a14bd7d8', null, 204);

String _month(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}';

void main() {
  testWidgets('renders the grouped debts and requests the current month', (
    tester,
  ) async {
    final api = _api();
    await pumpScreen(tester, const DebtListScreen(), api: api);
    await settle(tester);

    expect(find.text('Rent'), findsOneWidget);
    expect(find.text('Home'), findsWidgets);
    final q = api.requests.firstWhere((r) => r.path == '/debt/get').query;
    expect(q['month'], _month(DateTime.now()));
    expect(q['isOwner'], true);
  });

  testWidgets('previous-month chevron refetches the previous month', (
    tester,
  ) async {
    final api = _api();
    await pumpScreen(tester, const DebtListScreen(), api: api);
    await settle(tester);

    await tester.tap(find.byIcon(Icons.chevron_left));
    await settle(tester);

    final now = DateTime.now();
    final prev = DateTime(now.year, now.month - 1);
    final fetches = api.requests.where((r) => r.path == '/debt/get');
    expect(fetches.last.query['month'], _month(prev));
  });

  testWidgets('Shared tab requests debts the user does not own', (
    tester,
  ) async {
    final api = _api();
    await pumpScreen(tester, const DebtListScreen(), api: api);
    await settle(tester);

    await tester.tap(find.text('Shared'));
    await settle(tester);

    expect(api.requests.any((r) => r.path == '/debt/get' && r.query['isOwner'] == false), isTrue);
  });

  testWidgets('tapping a debt opens its detail route', (tester) async {
    await pumpScreen(tester, const DebtListScreen(), api: _api());
    await settle(tester);

    await tester.tap(find.text('Rent'));
    await settle(tester);

    expect(find.text('route:/debts/6afaf2bb7c4cf716a14bd7d9'), findsOneWidget);
  });

  testWidgets('category menu offers edit/transfer/delete', (tester) async {
    await pumpScreen(tester, const DebtListScreen(), api: _api());
    await settle(tester);

    await tester.tap(find.byIcon(Icons.more_vert).first);
    await settle(tester);

    expect(find.text('Edit Category'), findsOneWidget);
    expect(find.text('Transfer Debts & Delete'), findsOneWidget);
    expect(find.text('Delete with Debts'), findsOneWidget);
  });

  testWidgets('delete with debts asks for confirmation then calls the API', (
    tester,
  ) async {
    final api = _api();
    await pumpScreen(tester, const DebtListScreen(), api: api);
    await settle(tester);

    await tester.tap(find.byIcon(Icons.more_vert).first);
    await settle(tester);
    await tester.tap(find.text('Delete with Debts'));
    await settle(tester);

    expect(
      find.text('Category "Home" and all its debts will be deleted. This cannot be undone.'),
      findsOneWidget,
    );
    expect(
      api.requests.any((r) => r.method == 'DELETE'),
      isFalse,
      reason: 'nothing is deleted before the user confirms',
    );
    await tester.tap(find.text('Delete').last);
    await settle(tester);

    expect(
      api.requests.where((r) => r.method == 'DELETE').map((r) => r.path),
      contains('/category/delete/6afaf2bb7c4cf716a14bd7d8'),
    );
  });
}
