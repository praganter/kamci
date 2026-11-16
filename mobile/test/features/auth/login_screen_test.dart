import 'package:dt_mobile/core/l10n/l10n.dart';
import 'package:dt_mobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:dt_mobile/features/auth/presentation/providers/auth_state.dart';
import 'package:dt_mobile/features/auth/presentation/screens/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _RecordingAuth extends AuthNotifier {
  final logins = <(String, String)>[];

  @override
  AuthState build() => const AuthState.unauthenticated();

  @override
  Future<void> login(String email, String password) async {
    logins.add((email, password));
    state = const AuthState.error('Invalid email or password');
  }
}

Future<_RecordingAuth> _pump(WidgetTester tester) async {
  final auth = _RecordingAuth();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [authNotifierProvider.overrideWith(() => auth)],
      child: MaterialApp(
        locale: const Locale('en'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: const LoginScreen(),
      ),
    ),
  );
  return auth;
}

void main() {
  final l10n = lookupAppLocalizations(const Locale('en'));

  testWidgets('empty submit shows validation errors and does not call login', (
    tester,
  ) async {
    final auth = await _pump(tester);

    await tester.tap(find.text(l10n.signIn));
    await tester.pump();

    expect(find.text(l10n.validationEmailRequired), findsOneWidget);
    expect(find.text(l10n.validationPasswordRequired), findsOneWidget);
    expect(auth.logins, isEmpty);
  });

  testWidgets('malformed email and short password are rejected client-side', (
    tester,
  ) async {
    final auth = await _pump(tester);

    await tester.enterText(find.byType(TextFormField).at(0), 'not-an-email');
    await tester.enterText(find.byType(TextFormField).at(1), 'short');
    await tester.tap(find.text(l10n.signIn));
    await tester.pump();

    expect(find.text(l10n.validationEmailInvalid), findsOneWidget);
    expect(find.text(l10n.validationPasswordMinLength), findsOneWidget);
    expect(auth.logins, isEmpty);
  });

  testWidgets('valid form submits trimmed email and shows the error snackbar', (
    tester,
  ) async {
    final auth = await _pump(tester);

    await tester.enterText(
      find.byType(TextFormField).at(0),
      '  me@example.com ',
    );
    await tester.enterText(find.byType(TextFormField).at(1), 'correct-horse');
    await tester.tap(find.text(l10n.signIn));
    await tester.pump();
    await tester.pump();

    expect(auth.logins, [('me@example.com', 'correct-horse')]);
    expect(find.text('Invalid email or password'), findsOneWidget);
  });
}
