import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/providers/auth_state.dart';
import '../../features/auth/presentation/screens/change_password_screen.dart';
import '../../features/auth/presentation/screens/forgot_password_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/register_screen.dart';
import '../../features/auth/presentation/screens/reset_password_screen.dart';
import '../../features/auth/presentation/screens/splash_screen.dart';
import '../../features/category/presentation/screens/category_detail_screen.dart';
import '../../features/category/presentation/screens/category_list_screen.dart';
import '../../features/charts/presentation/screens/charts_screen.dart';
import '../../features/debt/presentation/screens/debt_detail_screen.dart';
import '../../features/debt/presentation/screens/debt_form_screen.dart';
import '../../features/debt/presentation/screens/debt_list_screen.dart';
import '../../features/household/presentation/screens/household_screen.dart';
import '../../features/income/presentation/screens/income_screen.dart';
import '../../features/notifications/presentation/screens/notification_list_screen.dart';
import '../../features/settings/presentation/screens/settings_screen.dart';
import '../../features/subscription/presentation/screens/premium_screen.dart';
import '../../shared/widgets/bottom_nav_bar.dart';
import 'route_names.dart';

part 'app_router.g.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _shellNavigatorKey = GlobalKey<NavigatorState>();

class _RouterNotifier extends ChangeNotifier {
  _RouterNotifier(Ref ref) {
    ref.listen<AuthState>(authNotifierProvider, (_, _) => notifyListeners());
  }
}

@Riverpod(keepAlive: true)
GoRouter appRouter(Ref ref) {
  final notifier = _RouterNotifier(ref);
  ref.onDispose(notifier.dispose);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: RoutePaths.splash,
    debugLogDiagnostics: kDebugMode,
    refreshListenable: notifier,
    redirect: (context, state) {
      if (state.uri.scheme == 'kamci') {
        if (state.uri.host == 'reset-password') {
          final token = state.uri.queryParameters['token'];
          return token != null
              ? '${RoutePaths.resetPassword}?token=$token'
              : RoutePaths.resetPassword;
        }
        return RoutePaths.login;
      }

      final authState = ref.read(authNotifierProvider);

      final isAuthRoute = [
        RoutePaths.splash,
        RoutePaths.login,
        RoutePaths.register,
        RoutePaths.forgotPassword,
        RoutePaths.resetPassword,
      ].any((path) => state.matchedLocation.startsWith(path));

      return switch (authState) {
        AuthStateInitial() ||
        AuthStateLoading() => isAuthRoute ? null : RoutePaths.splash,
        AuthStateAuthenticated() =>
          isAuthRoute && state.matchedLocation != RoutePaths.resetPassword
              ? RoutePaths.debts
              : null,
        AuthStateUnauthenticated() || AuthStateError() =>
          (!isAuthRoute || state.matchedLocation == RoutePaths.splash)
              ? RoutePaths.login
              : null,
      };
    },
    routes: [
      GoRoute(
        path: RoutePaths.splash,
        name: RouteNames.splash,
        builder: (context, _) => const SplashScreen(),
      ),
      GoRoute(
        path: RoutePaths.login,
        name: RouteNames.login,
        builder: (context, _) => const LoginScreen(),
      ),
      GoRoute(
        path: RoutePaths.register,
        name: RouteNames.register,
        builder: (context, _) => const RegisterScreen(),
      ),
      GoRoute(
        path: RoutePaths.changePassword,
        name: RouteNames.changePassword,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const ChangePasswordScreen(),
      ),
      GoRoute(
        path: RoutePaths.income,
        name: RouteNames.income,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const IncomeScreen(),
      ),
      GoRoute(
        path: RoutePaths.premium,
        name: RouteNames.premium,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const PremiumScreen(),
      ),
      GoRoute(
        path: RoutePaths.household,
        name: RouteNames.household,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const HouseholdScreen(),
      ),
      GoRoute(
        path: RoutePaths.notifications,
        name: RouteNames.notifications,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const NotificationListScreen(),
      ),
      GoRoute(
        path: RoutePaths.forgotPassword,
        name: RouteNames.forgotPassword,
        builder: (context, _) => const ForgotPasswordScreen(),
      ),
      GoRoute(
        path: RoutePaths.resetPassword,
        name: RouteNames.resetPassword,
        builder: (_, state) =>
            ResetPasswordScreen(token: state.uri.queryParameters['token']),
      ),
      ShellRoute(
        navigatorKey: _shellNavigatorKey,
        builder: (context, state, child) => AppShell(child: child),
        routes: [
          GoRoute(
            path: RoutePaths.debts,
            name: RouteNames.debts,
            pageBuilder: (context, state) =>
                _fadePage(state, const DebtListScreen()),
            routes: [
              GoRoute(
                path: 'new',
                name: RouteNames.debtNew,
                parentNavigatorKey: _rootNavigatorKey,
                builder: (context, _) => const DebtFormScreen(),
              ),
              GoRoute(
                path: ':id',
                name: RouteNames.debtDetail,
                parentNavigatorKey: _rootNavigatorKey,
                builder: (_, state) =>
                    DebtDetailScreen(id: state.pathParameters['id']!),
                routes: [
                  GoRoute(
                    path: 'edit',
                    name: RouteNames.debtEdit,
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (_, state) =>
                        DebtFormScreen(debtId: state.pathParameters['id']),
                  ),
                ],
              ),
            ],
          ),
          GoRoute(
            path: RoutePaths.categories,
            name: RouteNames.categories,
            pageBuilder: (context, state) =>
                _fadePage(state, const CategoryListScreen()),
            routes: [
              GoRoute(
                path: ':id',
                name: RouteNames.categoryDetail,
                builder: (_, state) =>
                    CategoryDetailScreen(id: state.pathParameters['id']!),
              ),
            ],
          ),
          GoRoute(
            path: RoutePaths.charts,
            name: RouteNames.charts,
            pageBuilder: (context, state) =>
                _fadePage(state, const ChartsScreen()),
          ),
          GoRoute(
            path: RoutePaths.settings,
            name: RouteNames.settings,
            pageBuilder: (context, state) =>
                _fadePage(state, const SettingsScreen()),
          ),
        ],
      ),
    ],
  );
}

Page<void> _fadePage(GoRouterState state, Widget child) {
  return CustomTransitionPage<void>(
    key: state.pageKey,
    child: child,
    transitionDuration: const Duration(milliseconds: 200),
    reverseTransitionDuration: const Duration(milliseconds: 150),
    transitionsBuilder: (context, animation, _, child) => FadeTransition(
      opacity: CurvedAnimation(parent: animation, curve: Curves.easeOut),
      child: child,
    ),
  );
}
