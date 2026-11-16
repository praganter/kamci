import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/notifications/presentation/screens/notification_list_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

Map<String, Object?> _page(List<Map<String, Object?>> items) => {
  'content': items,
  'page': 0,
  'size': 20,
  'totalElements': items.length,
  'totalPages': 1,
};

Map<String, Object?> _n(String id, String type, {bool read = false, String? entityId}) => {
  'id': id,
  'type': type,
  'title': 'Title $id',
  'body': 'Body $id',
  'entityId': entityId,
  'read': read,
  'createdDate': '2026-11-16T15:10:35',
};

void main() {
  testWidgets('empty list shows the empty state', (t) async {
    final api = FakeApi().on('GET /notification/list', fixture('notification_list'));
    final h = await pumpScreen(t, const NotificationListScreen(), api: api);
    await settle(t);
    expect(find.text(h.l10n.notificationsEmpty), findsOneWidget);
  });

  testWidgets('lists notifications with title, body and time', (t) async {
    final api = FakeApi().on(
      'GET /notification/list',
      _page([_n('a', 'WEEKLY_DIGEST'), _n('b', 'DEBT_REMINDER', read: true)]),
    );
    await pumpScreen(t, const NotificationListScreen(), api: api);
    await settle(t);
    expect(find.text('Title a'), findsOneWidget);
    expect(find.text('Body b'), findsOneWidget);
    expect(find.text('16 Nov, 15:10'), findsNWidgets(2));
  });

  testWidgets('tapping an unread notification marks it read and navigates', (t) async {
    final api = FakeApi()
        .on('GET /notification/list', _page([_n('a', 'DEBT_REMINDER', entityId: 'd1')]))
        .on('PATCH /notification/a/read', null, 204);
    await pumpScreen(t, const NotificationListScreen(), api: api);
    await settle(t);

    await t.tap(find.text('Title a'));
    await settle(t);
    expect(api.requests.any((r) => r.method == 'PATCH' && r.path == '/notification/a/read'), isTrue);
    expect(find.text('route:/debts/d1'), findsOneWidget);
  });

  testWidgets('tapping an already-read notification does not call the API', (t) async {
    final api = FakeApi().on('GET /notification/list', _page([_n('a', 'WEEKLY_DIGEST', read: true)]));
    await pumpScreen(t, const NotificationListScreen(), api: api);
    await settle(t);
    await t.tap(find.text('Title a'));
    await settle(t);
    expect(api.requests.where((r) => r.method == 'PATCH'), isEmpty);
  });

  testWidgets('a failed load shows the error text', (t) async {
    final api = FakeApi().on('GET /notification/list', {'message': 'x'}, 500);
    await pumpScreen(t, const NotificationListScreen(), api: api);
    await settle(t);
    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.byType(ListTile), findsNothing);
  });
}
