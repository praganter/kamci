import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/household/presentation/screens/household_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

Map<String, Object?> _hh(String status, {required bool owner}) => {
  'id': 'h1',
  'ownerId': 'o1',
  'ownerEmail': 'owner@example.com',
  'partnerId': 'p1',
  'partnerEmail': 'partner@example.com',
  'status': status,
  'owner': owner,
};

FakeApi _api(Object? household) =>
    FakeApi().on('GET /auth/household/', household, household == null ? 204 : 200);

void main() {
  testWidgets('an empty email is not sent', (t) async {
    final api = _api(null);
    await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);
    expect(find.text('Send Household Invite'), findsOneWidget);

    await t.tap(find.text('Send Invite'));
    await settle(t);
    expect(api.requests.where((r) => r.method == 'POST'), isEmpty);
  });

  testWidgets('a rejected invite keeps the form and shows no success', (t) async {
    final api = _api(null).on('POST /auth/household/invite', {
      'message': 'no',
    }, 400);
    final h = await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);

    await t.enterText(find.byType(TextField), 'partner@example.com');
    await t.tap(find.text('Send Invite'));
    await settle(t);
    expect(find.text(h.l10n.householdInviteSent), findsNothing);
    expect(find.text('Send Household Invite'), findsOneWidget);
  });

  testWidgets('a valid invite is posted and the pending state is shown', (t) async {
    final api = _api(null).on('POST /auth/household/invite', _hh('PENDING', owner: true));
    await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);

    await t.enterText(find.byType(TextField), 'partner@example.com');
    await t.tap(find.text('Send Invite'));
    await settle(t);
    expect(api.requests.firstWhere((r) => r.method == 'POST').data, {'email': 'partner@example.com'});
  });

  testWidgets('a pending invitation can be accepted', (t) async {
    final api = _api(_hh('PENDING', owner: false))
        .on('POST /auth/household/accept/h1', _hh('ACCEPTED', owner: false));
    await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);
    expect(find.text('owner@example.com invited you to their household'), findsOneWidget);

    await t.tap(find.text('Accept'));
    await settle(t);
    expect(api.requests.any((r) => r.method == 'POST' && r.path == '/auth/household/accept/h1'), isTrue);
  });

  testWidgets('a pending invitation can be declined', (t) async {
    final api = _api(_hh('PENDING', owner: false)).on('POST /auth/household/decline/h1', null, 204);
    await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);

    await t.tap(find.text('Decline'));
    await settle(t);
    expect(api.requests.any((r) => r.path == '/auth/household/decline/h1'), isTrue);
  });

  testWidgets('an accepted household shows the partner and can be left', (t) async {
    final api = _api(_hh('ACCEPTED', owner: true)).on('DELETE /auth/household/', null, 204);
    await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);
    expect(find.text('Shared household with partner@example.com'), findsOneWidget);

    await t.tap(find.text('Leave Household'));
    await settle(t);
    expect(api.requests.where((r) => r.method == 'DELETE'), isEmpty);
    await t.tap(find.text('Leave Household').last);
    await settle(t);
    expect(api.requests.any((r) => r.method == 'DELETE' && r.path == '/auth/household/'), isTrue);
  });

  testWidgets('a load failure does not crash the screen', (t) async {
    final api = FakeApi().on('GET /auth/household/', {'message': 'x'}, 500);
    await pumpScreen(t, const HouseholdScreen(), api: api);
    await settle(t);
    expect(find.byType(Scaffold), findsWidgets);
  });
}
