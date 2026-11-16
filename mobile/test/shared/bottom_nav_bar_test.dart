import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:kamci/core/l10n/l10n.dart';
import 'package:kamci/core/network/dio_client.dart';
import 'package:kamci/core/providers/app_settings_provider.dart';
import 'package:kamci/shared/widgets/bottom_nav_bar.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../helpers/fake_api.dart';

Future<GoRouter> _pump(WidgetTester tester, {String initial = '/debts'}) async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();
  final api = FakeApi().on('GET /category/getAll', fixture('category_get_all'));
  final router = GoRouter(
    initialLocation: initial,
    routes: [
      ShellRoute(
        builder: (_, _, child) => AppShell(child: child),
        routes: [
          for (final n in ['debts', 'categories', 'charts', 'settings'])
            GoRoute(
              name: n,
              path: '/$n',
              builder: (_, _) => Center(child: Text('page:$n')),
            ),
        ],
      ),
    ],
  );
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        dioClientProvider.overrideWithValue(api.dio()),
      ],
      child: MaterialApp.router(
        routerConfig: router,
        locale: const Locale('en'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
      ),
    ),
  );
  await tester.pump();
    await tester.pump(const Duration(seconds: 1));
  return router;
}

void main() {
  testWidgets('shows the four tabs and the first page', (tester) async {
    await _pump(tester);
    for (final label in ['Debts', 'Categories', 'Charts', 'Settings']) {
      expect(find.text(label), findsOneWidget);
    }
    expect(find.text('page:debts'), findsOneWidget);
  });

  testWidgets('tapping a tab navigates to its page', (tester) async {
    final router = await _pump(tester);

    await tester.tap(find.text('Charts'));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('page:charts'), findsOneWidget);
    expect(router.routerDelegate.currentConfiguration.uri.path, '/charts');

    await tester.tap(find.text('Settings'));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('page:settings'), findsOneWidget);

    await tester.tap(find.text('Categories'));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('page:categories'), findsOneWidget);

    await tester.tap(find.text('Debts'));
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('page:debts'), findsOneWidget);
  });

  testWidgets('the selected tab follows the current route', (tester) async {
    await _pump(tester, initial: '/categories');
    final selected = tester
        .widgetList<Semantics>(find.byType(Semantics))
        .where((s) => s.properties.identifier == 'navCategories')
        .single;
    expect(selected.properties.selected, isTrue);
  });
}
