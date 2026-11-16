import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:purchases_flutter/purchases_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';
import 'core/providers/app_settings_provider.dart';
import 'core/services/notification_service.dart';
import 'features/subscription/subscription_constants.dart';

void main() {
  runZonedGuarded(_bootstrap, (error, stack) {
    debugPrint('[UNHANDLED] $error\n$stack');
  });
}

Future<void> _bootstrap() async {
  // flutter run --dart-define=MAESTRO=true ile build edilince animasyonlar 100x hızlanır
  if (const bool.fromEnvironment('MAESTRO')) {
    timeDilation = 0.01;
  }
  WidgetsFlutterBinding.ensureInitialized();
  FlutterError.onError = (details) {
    debugPrint('[FLUTTER_ERROR] ${details.exception}\n${details.stack}');
  };
  // Firebase is optional: without google-services.json / GoogleService-Info.plist
  // the app runs normally and push notifications are simply disabled.
  try {
    await Firebase.initializeApp();
    NotificationService.enabled = true;
    FirebaseMessaging.onBackgroundMessage(firebaseBackgroundHandler);
    await NotificationService.initialize();
  } catch (e) {
    NotificationService.enabled = false;
    debugPrint('[FIREBASE] not configured, push disabled: $e');
  }
  // Key yoksa (dart-define verilmemiş) RevenueCat kapalı; satın alma ekranı
  // "mağaza yok" gösterir. Hata uygulamanın açılmasını engellememeli.
  final rcKey = SubscriptionConstants.revenueCatApiKey;
  if (rcKey.isNotEmpty) {
    try {
      await Purchases.configure(PurchasesConfiguration(rcKey));
    } catch (e) {
      debugPrint('[REVENUECAT] configure failed: $e');
    }
  }
  final prefs = await SharedPreferences.getInstance();
  runApp(
    ProviderScope(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      child: const App(),
    ),
  );
}
