import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/core/l10n/l10n.dart';
import 'package:kamci/shared/widgets/confirm_dialog.dart';

class _Host {
  bool? result;

  Future<void> pump(
    WidgetTester tester,
    Locale locale, {
    String? confirmText,
  }) async {
    await tester.pumpWidget(
      MaterialApp(
        locale: locale,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Builder(
          builder: (context) => TextButton(
            onPressed: () async => result = await showConfirmDialog(
              context,
              title: 'T',
              content: 'C',
              confirmText: confirmText,
            ),
            child: const Text('open'),
          ),
        ),
      ),
    );
    await open(tester);
  }

  Future<void> open(WidgetTester tester) async {
    await tester.tap(find.text('open'));
    await tester.pump(const Duration(milliseconds: 500));
  }
}

void main() {
  testWidgets('default labels follow the English locale', (tester) async {
    await _Host().pump(tester, const Locale('en'));
    expect(find.text('Delete'), findsOneWidget);
    expect(find.text('Cancel'), findsOneWidget);
  });

  testWidgets('default labels follow the Turkish locale', (tester) async {
    await _Host().pump(tester, const Locale('tr'));
    expect(find.text('Sil'), findsOneWidget);
    expect(find.text('İptal'), findsOneWidget);
  });

  testWidgets('explicit labels override the defaults', (tester) async {
    await _Host().pump(tester, const Locale('en'), confirmText: 'Leave');
    expect(find.text('Leave'), findsOneWidget);
    expect(find.text('Delete'), findsNothing);
  });

  testWidgets('cancel resolves false, confirm resolves true', (tester) async {
    final host = _Host();
    await host.pump(tester, const Locale('en'));
    await tester.tap(find.text('Cancel'));
    await tester.pump(const Duration(milliseconds: 500));
    expect(host.result, isFalse);

    await host.open(tester);
    await tester.tap(find.text('Delete'));
    await tester.pump(const Duration(milliseconds: 500));
    expect(host.result, isTrue);
  });
}
