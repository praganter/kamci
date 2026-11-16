import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/auth/presentation/widgets/auth_button.dart';
import 'package:kamci/features/auth/presentation/screens/change_password_screen.dart';
import 'package:kamci/features/auth/presentation/screens/forgot_password_screen.dart';
import 'package:kamci/features/auth/presentation/screens/register_screen.dart';
import 'package:kamci/features/auth/presentation/screens/reset_password_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

Future<void> _fill(WidgetTester t, List<String> values) async {
  for (var i = 0; i < values.length; i++) {
    await t.enterText(find.byType(TextFormField).at(i), values[i]);
  }
}

void main() {
  group('ForgotPasswordScreen', () {
    testWidgets('invalid email is rejected without calling the API', (t) async {
      final api = FakeApi();
      final h = await pumpScreen(t, const ForgotPasswordScreen(), api: api, authenticated: false);
      await _fill(t, ['nope']);
      await t.tap(find.text('Send'));
      await settle(t);
      expect(find.text(h.l10n.validationEmailInvalid), findsOneWidget);
      expect(api.requests, isEmpty);
    });

    testWidgets('valid email posts to forget-password', (t) async {
      final api = FakeApi().on('POST /auth/forget-password', null, 204);
      await pumpScreen(t, const ForgotPasswordScreen(), api: api, authenticated: false);
      await _fill(t, ['me@example.com']);
      await t.tap(find.text('Send'));
      await settle(t);
      expect(api.last.path, '/auth/forget-password');
      expect(api.last.data, {'email': 'me@example.com'});
    });
  });

  group('RegisterScreen', () {
    testWidgets('mismatched confirmation blocks submit', (t) async {
      final api = FakeApi();
      final h = await pumpScreen(t, const RegisterScreen(), api: api, authenticated: false);
      await _fill(t, ['me@example.com', 'password1', 'password2']);
      await t.tap(find.byType(AuthButton));
      await settle(t);
      expect(find.text(h.l10n.validationPasswordMismatch), findsOneWidget);
      expect(api.requests, isEmpty);
    });

    testWidgets('valid form registers and stores the session', (t) async {
      final api = FakeApi().on('POST /auth/register', fixture('auth_refresh'));
      await pumpScreen(t, const RegisterScreen(), api: api, authenticated: false);
      await _fill(t, ['me@example.com', 'password1', 'password1']);
      await t.tap(find.byType(AuthButton));
      await settle(t);
      expect(api.requests.first.path, '/auth/register');
      expect(api.requests.first.data, {'email': 'me@example.com', 'password': 'password1'});
    });
  });

  group('ResetPasswordScreen', () {
    testWidgets('sends the deep-link token with the new password', (t) async {
      final api = FakeApi().on('POST /auth/reset-password', null, 204);
      await pumpScreen(t, const ResetPasswordScreen(token: 'tok-1'), api: api, authenticated: false);
      await _fill(t, ['newpass123', 'newpass123']);
      await t.tap(find.text('Update Password'));
      await settle(t);
      expect(api.last.data, {'token': 'tok-1', 'newPassword': 'newpass123'});
    });

    testWidgets('short password is rejected client-side', (t) async {
      final api = FakeApi();
      final h = await pumpScreen(t, const ResetPasswordScreen(token: 'tok-1'), api: api, authenticated: false);
      await _fill(t, ['short', 'short']);
      await t.tap(find.text('Update Password'));
      await settle(t);
      expect(find.text(h.l10n.validationPasswordMinLength), findsOneWidget);
      expect(api.requests, isEmpty);
    });
  });

  group('ChangePasswordScreen', () {
    testWidgets('posts current and new password', (t) async {
      final api = FakeApi().on('POST /auth/change-password', null, 204);
      await pumpScreen(t, const ChangePasswordScreen(), api: api);
      await _fill(t, ['oldpass123', 'newpass123']);
      await t.tap(find.text('Update Password'));
      await settle(t);
      expect(api.last.data, {'currentPassword': 'oldpass123', 'newPassword': 'newpass123'});
      expect(find.text('host'), findsOneWidget, reason: 'success pops the screen');
    });

    testWidgets('server rejection leaves the form in place', (t) async {
      final api = FakeApi().on('POST /auth/change-password', {
        'code': 'error.auth.current_password_invalid',
        'message': 'wrong',
      }, 400);
      await pumpScreen(t, const ChangePasswordScreen(), api: api);
      await _fill(t, ['oldpass123', 'newpass123']);
      await t.tap(find.text('Update Password'));
      await settle(t);
      expect(api.last.path, '/auth/change-password');
      expect(find.byType(TextFormField), findsNWidgets(2));
    });
  });
}
