import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'core/l10n/l10n.dart';
import 'core/providers/app_settings_provider.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/constants/app_constants.dart';
import 'core/router/route_names.dart';

class App extends ConsumerStatefulWidget {
  const App({super.key});

  @override
  ConsumerState<App> createState() => _AppState();
}

class _AppState extends ConsumerState<App> {
  StreamSubscription<Uri>? _linkSub;

  @override
  void initState() {
    super.initState();
    _initDeepLinks();
  }

  Future<void> _initDeepLinks() async {
    final appLinks = AppLinks();

    // Cold start — app kapalıyken gelen link
    final initial = await appLinks.getInitialLink();
    if (initial != null) {
      _handleLink(initial);
    }

    // Uygulama açıkken gelen linkler
    _linkSub = appLinks.uriLinkStream.listen(_handleLink);
  }

  // Only allow Base64url + padding chars — prevents path/query injection via crafted deep links
  static final _tokenPattern = RegExp(r'^[A-Za-z0-9\-_.~+/=]+$');

  void _handleLink(Uri uri) {
    if (uri.scheme != AppConstants.deepLinkScheme) return;

    final router = ref.read(appRouterProvider);

    // debttracker://reset-password?token=xxx
    if (uri.host == 'reset-password') {
      final raw = uri.queryParameters['token'];
      final token = (raw != null && _tokenPattern.hasMatch(raw)) ? raw : null;
      router.goNamed(
        RouteNames.resetPassword,
        queryParameters: token != null ? {'token': token} : {},
      );
    }
  }

  @override
  void dispose() {
    _linkSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(appRouterProvider);
    final themeMode = ref.watch(themeModeProvider);
    final locale = ref.watch(localeProvider);

    Intl.defaultLocale = (locale ?? const Locale('tr')).toLanguageTag();

    return MaterialApp.router(
      title: AppConstants.appName,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: themeMode,
      locale: locale,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      debugShowCheckedModeBanner: false,
      routerConfig: router,
    );
  }
}
