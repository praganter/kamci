import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/features/auth/presentation/screens/splash_screen.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

void main() {
  testWidgets('tagline is localized', (t) async {
    await pumpScreen(t, const SplashScreen(), api: FakeApi(), authenticated: false);
    expect(find.text("Debt is the brave one's Kamci"), findsOneWidget);
  });

  testWidgets('tagline follows the Turkish locale', (t) async {
    await pumpScreen(
      t,
      const SplashScreen(),
      api: FakeApi(),
      authenticated: false,
      locale: const Locale('tr'),
    );
    expect(find.text("Borç yiğidin 'Kamci'sıdır"), findsOneWidget);
  });
}
