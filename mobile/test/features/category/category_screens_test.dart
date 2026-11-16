import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/category/presentation/screens/category_detail_screen.dart';
import 'package:kamci/features/category/presentation/screens/category_list_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

const _owner = '6afaf2bb399846caf42061a0';
const _catId = '6afaf2bb7c4cf716a14bd7d8';

FakeApi _api({List<Object?>? categories}) => FakeApi()
    .on('GET /category/getAll', categories ?? fixture('category_get_all'))
    .on('GET /auth/user-detail', fixture('auth_user_detail'));

Map<String, Object?> _shared() => {
  'id': _catId,
  'name': 'Home',
  'ownerId': _owner,
  'members': [
    {'userId': 'u2', 'email': 'friend@example.com', 'accessLevel': 'READ'},
    {'userId': 'u3', 'email': 'editor@example.com', 'accessLevel': 'EDIT'},
  ],
};

void main() {
  group('CategoryListScreen', () {
    testWidgets('owner sees their categories; tapping opens the detail route', (t) async {
      await pumpScreen(t, const CategoryListScreen(), api: _api(), userId: _owner);
      await settle(t);
      expect(find.text('Home'), findsOneWidget);
      await t.tap(find.text('Home'));
      await settle(t);
      expect(find.text('route:/categories/$_catId'), findsOneWidget);
    });

    testWidgets('categories owned by someone else are not in My Categories', (t) async {
      await pumpScreen(t, const CategoryListScreen(), api: _api(), userId: 'someone-else');
      await settle(t);
      expect(find.text('Home'), findsNothing);
    });

    testWidgets('delete asks for confirmation then calls the API', (t) async {
      final api = _api().on('DELETE /category/delete/$_catId', null, 204);
      final h = await pumpScreen(t, const CategoryListScreen(), api: api, userId: _owner);
      await settle(t);

      await t.tap(find.byIcon(Icons.more_vert));
      await settle(t);
      await t.tap(find.text(h.l10n.delete));
      await settle(t);
      expect(find.text(h.l10n.deleteCategoryConfirm('Home')), findsOneWidget);
      expect(api.requests.where((r) => r.method == 'DELETE'), isEmpty);

      await t.tap(find.text('Delete').last);
      await settle(t);
      expect(api.requests.where((r) => r.method == 'DELETE').single.path, '/category/delete/$_catId');
    });

    testWidgets('FAB creates a category with the typed name', (t) async {
      final api = _api().on('POST /category/save', fixture('category_save'));
      final h = await pumpScreen(t, const CategoryListScreen(), api: api, userId: _owner);
      await settle(t);

      await t.tap(find.byIcon(Icons.add));
      await settle(t);
      await t.tap(find.text(h.l10n.create));
      await settle(t);
      expect(find.text(h.l10n.validationFieldRequired(h.l10n.categoryName)), findsOneWidget);
      expect(api.requests.where((r) => r.method == 'POST'), isEmpty);

      await t.enterText(find.byType(TextFormField), 'Travel');
      await t.tap(find.text(h.l10n.create));
      await settle(t);
      expect(api.requests.firstWhere((r) => r.path == '/category/save').data, {'name': 'Travel'});
    });

    testWidgets('edit prefills the name and posts an update', (t) async {
      final api = _api().on('POST /category/update', fixture('category_save'));
      final h = await pumpScreen(t, const CategoryListScreen(), api: api, userId: _owner);
      await settle(t);

      await t.tap(find.byIcon(Icons.more_vert));
      await settle(t);
      await t.tap(find.text(h.l10n.edit));
      await settle(t);
      expect(find.widgetWithText(TextFormField, 'Home'), findsOneWidget);
      await t.enterText(find.byType(TextFormField), 'Flat');
      await t.tap(find.text(h.l10n.update));
      await settle(t);
      expect(api.requests.firstWhere((r) => r.path == '/category/update').data, {'id': _catId, 'name': 'Flat'});
    });

    testWidgets('load failure renders an error with retry', (t) async {
      final api = FakeApi().on('GET /category/getAll', {'message': 'x'}, 500);
      await pumpScreen(t, const CategoryListScreen(), api: api, userId: _owner);
      await settle(t);
      final before = api.requests.length;
      await t.tap(find.byType(FilledButton));
      await settle(t);
      expect(api.requests.length, greaterThan(before));
    });
  });

  group('CategoryDetailScreen', () {
    testWidgets('without members shows the empty state', (t) async {
      final h = await pumpScreen(t, const CategoryDetailScreen(id: _catId), api: _api(), userId: _owner);
      await settle(t);
      expect(find.text(h.l10n.noMembersYet), findsOneWidget);
    });

    testWidgets('lists members grouped by access level', (t) async {
      final api = _api(categories: [_shared()]);
      await pumpScreen(t, const CategoryDetailScreen(id: _catId), api: api, userId: _owner);
      await settle(t);
      expect(find.text('friend@example.com'), findsOneWidget);
      expect(find.text('editor@example.com'), findsOneWidget);
    });

    testWidgets('removing a member confirms first, then calls DELETE', (t) async {
      final api = _api(categories: [_shared()]).on('DELETE /category/$_catId/member/u2', null, 204);
      final h = await pumpScreen(t, const CategoryDetailScreen(id: _catId), api: api, userId: _owner);
      await settle(t);

      await t.tap(find.byIcon(Icons.person_remove_outlined).first);
      await settle(t);
      expect(api.requests.where((r) => r.method == 'DELETE'), isEmpty);
      await t.tap(find.text(h.l10n.remove));
      await settle(t);
      expect(api.requests.where((r) => r.method == 'DELETE').single.path.startsWith('/category/$_catId/member/'), isTrue);
    });

    testWidgets('non-owner without edit rights gets no management actions', (t) async {
      final api = _api(categories: [_shared()]);
      await pumpScreen(t, const CategoryDetailScreen(id: _catId), api: api, userId: 'u2');
      await settle(t);
      expect(find.byIcon(Icons.person_add_outlined), findsNothing);
      expect(find.byIcon(Icons.delete_outline), findsNothing);
    });
  });
}
