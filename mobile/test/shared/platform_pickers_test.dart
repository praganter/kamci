import 'package:kamci/shared/widgets/platform_date_field.dart';
import 'package:kamci/shared/widgets/platform_time_field.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('seçici kapanınca text alanı yeniden odak almaz', (tester) async {
    final focus = FocusNode();
    addTearDown(focus.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Column(
            children: [
              TextField(focusNode: focus),
              PlatformDateField(label: 'Tarih', value: null, onChanged: (_) {}),
              PlatformTimeField(label: 'Saat', value: null, onChanged: (_) {}),
            ],
          ),
        ),
      ),
    );

    for (final label in ['Tarih', 'Saat']) {
      await tester.tap(find.byType(TextField));
      await tester.pump();
      expect(focus.hasFocus, isTrue);

      await tester.tap(find.text(label));
      await tester.pumpAndSettle();
      await tester.tap(find.text('OK'));
      await tester.pumpAndSettle();
      expect(focus.hasFocus, isFalse, reason: label);
    }
  });
}
