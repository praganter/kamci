import 'package:dt_mobile/features/category/domain/entities/member_embedded.dart';
import 'package:dt_mobile/features/category/presentation/widgets/access_level_field.dart';
import 'package:dt_mobile/gen/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('dar diyalogda taşma yok, kapalıyken sadece seviye adı', (
    tester,
  ) async {
    AccessLevel selected = AccessLevel.read;
    await tester.pumpWidget(
      MaterialApp(
        locale: const Locale('tr'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 260,
              child: StatefulBuilder(
                builder: (context, setState) => AccessLevelField(
                  value: selected,
                  prefixIcon: const Icon(Icons.security_outlined),
                  onChanged: (v) => setState(() => selected = v),
                ),
              ),
            ),
          ),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('Okuma'), findsOneWidget);
    expect(find.text('Sadece görüntüleyebilir'), findsNothing);

    await tester.tap(find.text('Okuma'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(
      find.text('Tam yetki: görüntüleme, ödeme, düzenleme ve silme'),
      findsOneWidget,
    );

    await tester.tap(find.text('Admin').last);
    await tester.pumpAndSettle();
    expect(selected, AccessLevel.admin);
    expect(tester.takeException(), isNull);
  });
}
