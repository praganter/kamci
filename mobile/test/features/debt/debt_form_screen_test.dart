import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/debt/presentation/screens/debt_form_screen.dart';
import 'package:kamci/shared/widgets/platform_select_field.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

const _owner = '6afaf2bb399846caf42061a0';
const _id = '6afaf2bb7c4cf716a14bd7d9';

FakeApi _api() => FakeApi()
    .on('GET /category/getAll', fixture('category_get_all'))
    .on('GET /auth/user-detail', fixture('auth_user_detail'))
    .on('GET /debt/get', fixture('debt_get_grouped'))
    .on('GET /debt/$_id', fixture('debt_by_id'))
    .on('POST /debt/save', fixture('debt_save'))
    .on('POST /debt/update', fixture('debt_save'));

void main() {
  testWidgets('empty submit shows validation errors and sends nothing', (t) async {
    final api = _api();
    await pumpScreen(t, const DebtFormScreen(), api: api, userId: _owner);
    await settle(t);

    await t.tap(find.text('Create'));
    await settle(t);

    expect(find.byType(TextFormField), findsWidgets);
    expect(api.requests.where((r) => r.method == 'POST'), isEmpty);
    expect(find.textContaining('required'), findsWidgets);
  });

  testWidgets('a valid form creates the debt with the typed values', (t) async {
    final api = _api();
    await pumpScreen(t, const DebtFormScreen(), api: api, userId: _owner);
    await settle(t);

    await t.enterText(find.byType(TextFormField).at(0), 'Laptop');
    await t.enterText(find.byType(TextFormField).at(1), '1250,50');
    await t.tap(find.byType(PlatformSelectField<String>).first);
    await settle(t);
    await t.tap(find.text('Home').last);
    await settle(t);
    await t.tap(find.text('Create'));
    await settle(t);

    final save = api.requests.firstWhere((r) => r.path == '/debt/save').data as Map;
    expect(save['name'], 'Laptop');
    expect(save['amount'], 1250.5);
    expect(save['categoryId'], '6afaf2bb7c4cf716a14bd7d8');
    expect(save['paymentPeriodType'], 'MONTHLY');
    expect(save['autoPayment'], false);
    expect(save.containsKey('reminder'), isFalse);
    expect(find.text('host'), findsOneWidget, reason: 'success closes the form');
  });

  testWidgets('editing prefills the form from the existing debt and updates it', (t) async {
    final api = _api();
    await pumpScreen(t, const DebtFormScreen(debtId: _id), api: api, userId: _owner);
    await settle(t);

    expect(find.widgetWithText(TextFormField, 'Rent'), findsOneWidget);
    await t.enterText(find.byType(TextFormField).at(0), 'Rent 2');
    await t.tap(find.text('Update'));
    await settle(t);

    final upd = api.requests.firstWhere((r) => r.path == '/debt/update').data as Map;
    expect(upd['id'], _id);
    expect(upd['name'], 'Rent 2');
    expect(upd['amount'], 500.0);
  });

  testWidgets('the free-tier limit opens the premium sheet instead of closing', (t) async {
    final api = _api().on('POST /debt/save', {
      'code': 'error.debt.limit_exceeded',
      'message': 'limit',
    }, 403);
    await pumpScreen(t, const DebtFormScreen(), api: api, userId: _owner);
    await settle(t);

    await t.enterText(find.byType(TextFormField).at(0), 'Laptop');
    await t.enterText(find.byType(TextFormField).at(1), '10');
    await t.tap(find.byType(PlatformSelectField<String>).first);
    await settle(t);
    await t.tap(find.text('Home').last);
    await settle(t);
    await t.tap(find.text('Create'));
    await settle(t);

    expect(find.text('host'), findsNothing);
    expect(find.byType(TextFormField), findsWidgets);
    expect(find.textContaining('Premium'), findsWidgets);
  });

  testWidgets('server errors keep the form open', (t) async {
    final api = _api().on('POST /debt/save', {'message': 'boom'}, 500);
    await pumpScreen(t, const DebtFormScreen(), api: api, userId: _owner);
    await settle(t);
    await t.enterText(find.byType(TextFormField).at(0), 'Laptop');
    await t.enterText(find.byType(TextFormField).at(1), '10');
    await t.tap(find.byType(PlatformSelectField<String>).first);
    await settle(t);
    await t.tap(find.text('Home').last);
    await settle(t);
    await t.tap(find.text('Create'));
    await settle(t);
    expect(find.text('host'), findsNothing);
  });
}
