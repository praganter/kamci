import 'package:dt_mobile/core/router/route_names.dart';
import 'package:dt_mobile/core/services/notification_routing.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('notificationTarget (in-app liste: type + entityId)', () {
    test('DEBT_REMINDER + id → borç detayı', () {
      expect(
        notificationTarget(type: 'DEBT_REMINDER', entityId: 'abc123'),
        const NotificationTarget(RouteNames.debtDetail, {'id': 'abc123'}),
      );
    });

    test('CATEGORY_MEMBER_ADDED + id → kategori detayı', () {
      expect(
        notificationTarget(type: 'CATEGORY_MEMBER_ADDED', entityId: 'c1'),
        const NotificationTarget(RouteNames.categoryDetail, {'id': 'c1'}),
      );
    });

    test('HOUSEHOLD_INVITE → Hane ekranı', () {
      expect(
        notificationTarget(type: 'HOUSEHOLD_INVITE', entityId: 'h1'),
        const NotificationTarget(RouteNames.household),
      );
    });

    test('WEEKLY_DIGEST (entityId null) → borç listesi sekmesi', () {
      final t = notificationTarget(type: 'WEEKLY_DIGEST');
      expect(t, const NotificationTarget(RouteNames.debts));
      expect(t!.isTab, isTrue);
    });

    test('tip büyük/küçük harf duyarsız', () {
      expect(
        notificationTarget(type: 'weekly_digest'),
        const NotificationTarget(RouteNames.debts),
      );
    });

    test('bilinmeyen tip → null', () {
      expect(notificationTarget(type: 'SOMETHING_NEW', entityId: 'x'), isNull);
      expect(notificationTarget(), isNull);
    });

    test('geçersiz id (path injection) → detay yerine liste', () {
      expect(
        notificationTarget(type: 'DEBT_REMINDER', entityId: '../settings'),
        const NotificationTarget(RouteNames.debts),
      );
    });
  });

  group('notificationTargetFromPush (FCM data payload)', () {
    test('debtId → borç detayı', () {
      expect(
        notificationTargetFromPush({'debtId': 'd1'}),
        const NotificationTarget(RouteNames.debtDetail, {'id': 'd1'}),
      );
    });

    test('categoryId → kategori detayı', () {
      expect(
        notificationTargetFromPush({'categoryId': 'c1'}),
        const NotificationTarget(RouteNames.categoryDetail, {'id': 'c1'}),
      );
    });

    test('householdId → Hane ekranı', () {
      expect(
        notificationTargetFromPush({'householdId': 'h1'}),
        const NotificationTarget(RouteNames.household),
      );
    });

    test('type: weekly_digest → borç listesi', () {
      expect(
        notificationTargetFromPush({'type': 'weekly_digest'}),
        const NotificationTarget(RouteNames.debts),
      );
    });

    test('boş payload → null', () {
      expect(notificationTargetFromPush({}), isNull);
    });
  });
}
