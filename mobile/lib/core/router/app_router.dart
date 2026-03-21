import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/providers/auth_state.dart';
import '../../features/auth/presentation/screens/splash_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/register_screen.dart';
import '../../features/auth/presentation/screens/forgot_password_screen.dart';
import '../../features/auth/presentation/screens/reset_password_screen.dart';
import '../../features/debt/presentation/screens/debt_list_screen.dart';
import '../../features/debt/presentation/screens/debt_detail_screen.dart';
import '../../features/debt/presentation/screens/debt_form_screen.dart';
import '../../features/category/presentation/screens/category_list_screen.dart';
import '../../features/category/presentation/screens/category_detail_screen.dart';
import '../../features/category/presentation/screens/category_form_screen.dart';
import '../../features/auth/presentation/screens/change_password_screen.dart';
import '../../features/settings/presentation/screens/settings_screen.dart';
import '../../shared/widgets/bottom_nav_bar.dart';
import 'route_names.dart';

part 'app_router.g.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _shellNavigatorKey = GlobalKey<NavigatorState>();

class _RouterNotifier extends ChangeNotifier {
  _RouterNotifier(Ref ref) {
    ref.listen<AuthState>(authNotifierProvider, (_, __) => notifyListeners());
  }
}

@Riverpod(keepAlive: true)
GoRouter appRouter(Ref ref) {
  final notifier = _RouterNotifier(ref);
  ref.onDispose(notifier.dispose);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: RoutePaths.splash,
    debugLogDiagnostics: true,
    refreshListenable: notifier,
    redirect: (context, state) {
      final authState = ref.read(authNotifierProvider);

      final isAuthRoute = [
        RoutePaths.splash,
        RoutePaths.login,
        RoutePaths.register,
        RoutePaths.forgotPassword,
        RoutePaths.resetPassword,
      ].any((path) => state.matchedLocation.startsWith(path));

      return switch (authState) {
        AuthStateInitial() || AuthStateLoading() =>
          isAuthRoute ? null : RoutePaths.splash,
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
        path: RoutePaths.settings,
        name: RouteNames.settings,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const SettingsScreen(),
      ),
      GoRoute(
        path: RoutePaths.changePassword,
        name: RouteNames.changePassword,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, _) => const ChangePasswordScreen(),
      ),
      GoRoute(
        path: RoutePaths.forgotPassword,
        name: RouteNames.forgotPassword,
        builder: (context, _) => const ForgotPasswordScreen(),
      ),
      GoRoute(
        path: RoutePaths.resetPassword,
        name: RouteNames.resetPassword,
        builder: (_, state) => ResetPasswordScreen(
          token: state.uri.queryParameters['token'],
        ),
      ),
      ShellRoute(
        navigatorKey: _shellNavigatorKey,
        builder: (context, state, child) => AppShell(child: child),
        routes: [
          GoRoute(
            path: RoutePaths.debts,
            name: RouteNames.debts,
            builder: (context, _) => const DebtListScreen(),
            routes: [
              GoRoute(
                path: 'new',
                name: RouteNames.debtNew,
                builder: (context, _) => const DebtFormScreen(),
              ),
              GoRoute(
                path: ':id',
                name: RouteNames.debtDetail,
                builder: (_, state) =>
                    DebtDetailScreen(id: state.pathParameters['id']!),
                routes: [
                  GoRoute(
                    path: 'edit',
                    name: RouteNames.debtEdit,
                    builder: (_, state) => DebtFormScreen(
                      debtId: state.pathParameters['id'],
                    ),
                  ),
                ],
              ),
            ],
          ),
          GoRoute(
            path: RoutePaths.categories,
            name: RouteNames.categories,
            builder: (context, _) => const CategoryListScreen(),
            routes: [
              GoRoute(
                path: 'new',
                name: RouteNames.categoryNew,
                builder: (context, _) => const CategoryFormScreen(),
              ),
              GoRoute(
                path: ':id',
                name: RouteNames.categoryDetail,
                builder: (_, state) =>
                    CategoryDetailScreen(id: state.pathParameters['id']!),
                routes: [
                  GoRoute(
                    path: 'edit',
                    name: RouteNames.categoryEdit,
                    builder: (_, state) => CategoryFormScreen(
                      categoryId: state.pathParameters['id'],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );
}
