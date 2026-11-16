import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/core/providers/app_settings_provider.dart';
import 'package:kamci/features/auth/presentation/providers/auth_provider.dart';
import 'package:kamci/features/auth/presentation/providers/auth_state.dart';
import 'package:kamci/features/settings/presentation/screens/settings_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

FakeApi _api() => FakeApi()
    .on('GET /auth/user-detail', fixture('auth_user_detail'))
    .on('POST /auth/logout', null, 204);

void main() {
  testWidgets('shows the signed-in email from user-detail', (tester) async {
    await pumpScreen(tester, const SettingsScreen(), api: _api());
    await settle(tester);
    expect(find.text(fixture('auth_user_detail')['email'] as String), findsOneWidget);
    expect(find.text('Get Premium'), findsOneWidget);
  });

  testWidgets('choosing Dark persists the theme preference', (tester) async {
    final h = await pumpScreen(tester, const SettingsScreen(), api: _api());
    await settle(tester);

    await tester.tap(find.text('Dark'));
    await settle(tester);

    expect(h.container.read(themeModeProvider), ThemeMode.dark);
    expect((await SharedPreferences.getInstance()).getString('theme_mode'), 'dark');
  });

  testWidgets('choosing Türkçe persists the locale preference', (tester) async {
    final h = await pumpScreen(tester, const SettingsScreen(), api: _api());
    await settle(tester);

    await tester.tap(find.text('Türkçe'));
    await settle(tester);

    expect(h.container.read(localeProvider), const Locale('tr'));
    expect((await SharedPreferences.getInstance()).getString('locale'), 'tr');
  });

  testWidgets('stored preferences are reflected on open', (tester) async {
    final h = await pumpScreen(
      tester,
      const SettingsScreen(),
      api: _api(),
      prefs: {'theme_mode': 'light', 'base_currency': 'USD'},
    );
    await settle(tester);
    expect(h.container.read(themeModeProvider), ThemeMode.light);
    expect(h.container.read(baseCurrencyProvider), 'USD');
  });

  testWidgets('sign out asks for confirmation and then logs out', (tester) async {
    final api = _api();
    final h = await pumpScreen(tester, const SettingsScreen(), api: api);
    await settle(tester);

    await tester.scrollUntilVisible(find.text('Sign Out'), 300);
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -200));
    await settle(tester);
    await tester.tap(find.text('Sign Out'));
    await settle(tester);
    expect(find.text(h.l10n.signOutConfirm), findsOneWidget);
    expect(api.requests.where((r) => r.path == '/auth/logout'), isEmpty);

    await tester.tap(find.text(h.l10n.signOut).last);
    await settle(tester);

    expect(h.container.read(authNotifierProvider), isA<AuthStateUnauthenticated>());
  });

  testWidgets('account rows navigate to their routes', (tester) async {
    await pumpScreen(tester, const SettingsScreen(), api: _api());
    await settle(tester);
    await tester.scrollUntilVisible(find.text('Income'), 300);
    await tester.tap(find.text('Income'));
    await settle(tester);
    expect(find.text('route:/income'), findsOneWidget);
  });
}
