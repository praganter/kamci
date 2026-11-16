import '../router/route_names.dart';

class NotificationTarget {
  const NotificationTarget(this.name, [this.pathParameters = const {}]);

  final String name;
  final Map<String, String> pathParameters;

  bool get isTab =>
      name == RouteNames.debts ||
      name == RouteNames.categories ||
      name == RouteNames.charts ||
      name == RouteNames.settings;

  @override
  bool operator ==(Object other) =>
      other is NotificationTarget &&
      other.name == name &&
      other.pathParameters.toString() == pathParameters.toString();

  @override
  int get hashCode => Object.hash(name, pathParameters.toString());

  @override
  String toString() => 'NotificationTarget($name, $pathParameters)';
}

final _idPattern = RegExp(r'^[A-Za-z0-9_-]{1,64}$');

NotificationTarget? notificationTarget({String? type, String? entityId}) {
  final id = entityId != null && _idPattern.hasMatch(entityId)
      ? entityId
      : null;
  switch (type?.toUpperCase()) {
    case 'DEBT_REMINDER':
    case 'DEBT_ADDED':
      return id == null
          ? const NotificationTarget(RouteNames.debts)
          : NotificationTarget(RouteNames.debtDetail, {'id': id});
    case 'CATEGORY_MEMBER_ADDED':
      return id == null
          ? const NotificationTarget(RouteNames.categories)
          : NotificationTarget(RouteNames.categoryDetail, {'id': id});
    case 'HOUSEHOLD_INVITE':
      return const NotificationTarget(RouteNames.household);
    case 'WEEKLY_DIGEST':
      return const NotificationTarget(RouteNames.debts);
  }
  return null;
}

NotificationTarget? notificationTargetFromPush(Map<String, dynamic> data) {
  String? str(String key) => data[key] is String ? data[key] as String : null;

  if (str('debtId') != null) {
    return notificationTarget(type: 'DEBT_REMINDER', entityId: str('debtId'));
  }
  if (str('categoryId') != null) {
    return notificationTarget(
      type: 'CATEGORY_MEMBER_ADDED',
      entityId: str('categoryId'),
    );
  }
  if (str('householdId') != null) {
    return notificationTarget(type: 'HOUSEHOLD_INVITE');
  }
  return notificationTarget(type: str('type'), entityId: str('entityId'));
}
