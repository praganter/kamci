import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:kamci/core/l10n/l10n.dart';
import 'package:kamci/core/network/dio_client.dart';
import 'package:kamci/core/providers/app_settings_provider.dart';
import 'package:kamci/core/storage/secure_storage_service.dart';
import 'package:kamci/features/auth/domain/entities/auth_tokens.dart';
import 'package:kamci/features/auth/presentation/providers/auth_provider.dart';
import 'package:kamci/features/auth/presentation/providers/auth_state.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'fake_api.dart';
import 'fakes.dart';

class _AuthedNotifier extends AuthNotifier {
  _AuthedNotifier(this.tokens);
  final AuthTokens tokens;

  @override
  AuthState build() => AuthState.authenticated(tokens);
}

class Harness {
  Harness(this.api, this.router, this.container);
  final FakeApi api;
  final GoRouter router;
  final ProviderContainer container;
  AppLocalizations get l10n => lookupAppLocalizations(const Locale('en'));
  String get location =>
      router.routerDelegate.currentConfiguration.uri.toString();
}

Future<Harness> pumpScreen(
  WidgetTester tester,
  Widget screen, {
  required FakeApi api,
  bool premium = false,
  bool authenticated = true,
  String userId = 'user-1',
  Map<String, Object> prefs = const {},
  List<Override> overrides = const [],
  Size size = const Size(700, 1200),
}) async {
  SharedPreferences.setMockInitialValues(prefs);
  final sp = await SharedPreferences.getInstance();
  tester.view.physicalSize = size * 2;
  tester.view.devicePixelRatio = 2;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);

  final prevOnError = FlutterError.onError;
  FlutterError.onError = (d) {
    if (d.exception.toString().contains('overflowed')) return;
    prevOnError?.call(d);
  };
  addTearDown(() => FlutterError.onError = prevOnError);

  final storage = FakeStorage();
  final tokens = AuthTokens(
    accessToken: fakeJwt(sub: userId, premium: premium),
    refreshToken: 'refresh',
  );
  final stubs = <String, String>{
    'splash': '/splash',
    'login': '/login',
    'register': '/register',
    'forgot-password': '/forgot-password',
    'reset-password': '/reset-password',
    'debts': '/debts',
    'debt-new': '/debts/new',
    'debt-detail': '/debts/:id',
    'debt-edit': '/debts/:id/edit',
    'categories': '/categories',
    'category-detail': '/categories/:id',
    'settings': '/settings',
    'change-password': '/change-password',
    'income': '/income',
    'charts': '/charts',
    'premium': '/premium',
    'household': '/household',
    'notifications': '/notifications',
  };
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => const Scaffold(body: Text('host')),
      ),
      GoRoute(path: '/_screen', builder: (_, _) => screen),
      for (final e in stubs.entries)
        GoRoute(
          name: e.key,
          path: e.value,
          builder: (_, s) => Scaffold(body: Text('route:${s.uri}')),
        ),
    ],
  );
  final container = ProviderContainer(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(sp),
      dioClientProvider.overrideWithValue(api.dio()),
      refreshDioProvider.overrideWithValue(api.dio()),
      secureStorageServiceProvider.overrideWithValue(storage),
      if (authenticated)
        authNotifierProvider.overrideWith(() => _AuthedNotifier(tokens)),
      ...overrides,
    ],
  );
  addTearDown(container.dispose);
  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp.router(
        routerConfig: router,
        locale: const Locale('en'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
      ),
    ),
  );
  router.push('/_screen');
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 400));
  return Harness(api, router, container);
}

Future<void> settle(WidgetTester tester) async {
  for (var i = 0; i < 6; i++) {
    await tester.pump(const Duration(milliseconds: 100));
  }
}
