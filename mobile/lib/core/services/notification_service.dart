import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../constants/api_constants.dart';
import '../network/dio_client.dart';

part 'notification_service.g.dart';

const _channelId = 'high_importance_channel';
const _channelName = 'High Importance Notifications';

final _localNotifications = FlutterLocalNotificationsPlugin();

/// Top-level background message handler — must be outside any class.
@pragma('vm:entry-point')
Future<void> firebaseBackgroundHandler(RemoteMessage message) async {
  // Firebase is already initialized in main.dart before this is called.
  debugPrint('FCM background message: ${message.messageId}');
}

@Riverpod(keepAlive: true)
NotificationService notificationService(Ref ref) => NotificationService(ref);

class NotificationService {
  NotificationService(this._ref) {
    FirebaseMessaging.instance.onTokenRefresh.listen((_) => registerToken());
  }

  final Ref _ref;

  /// Call once in main() after Firebase.initializeApp().
  static Future<void> initialize() async {
    // Request permission (iOS + Android 13+)
    await FirebaseMessaging.instance.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    // Create Android notification channel
    const channel = AndroidNotificationChannel(
      _channelId,
      _channelName,
      importance: Importance.high,
    );
    await _localNotifications
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >()
        ?.createNotificationChannel(channel);

    // Init flutter_local_notifications (Android only — iOS uses setForegroundNotificationPresentationOptions)
    const initSettings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      ),
    );
    await _localNotifications.initialize(initSettings);

    // iOS: show notification banner even when app is in foreground
    await FirebaseMessaging.instance
        .setForegroundNotificationPresentationOptions(
          alert: true,
          badge: true,
          sound: true,
        );

    // Show local notification when app is in foreground (Android)
    FirebaseMessaging.onMessage.listen(_showForegroundNotification);

    // Handle notification clicks when app is in background
    FirebaseMessaging.onMessageOpenedApp.listen(_handleNotificationClick);

    // Handle notification click when app is terminated
    FirebaseMessaging.instance.getInitialMessage().then((message) {
      if (message != null) {
        _handleNotificationClick(message);
      }
    });
  }

  static void _handleNotificationClick(RemoteMessage message) {
    debugPrint('Notification clicked: ${message.data}');
    // TODO: Add navigation logic based on message.data if needed
  }

  static void _showForegroundNotification(RemoteMessage message) {
    final notification = message.notification;
    if (notification == null) return;
    _localNotifications.show(
      notification.hashCode,
      notification.title,
      notification.body,
      const NotificationDetails(
        android: AndroidNotificationDetails(
          _channelId,
          _channelName,
          importance: Importance.high,
          priority: Priority.high,
          icon: '@mipmap/ic_launcher',
        ),
        iOS: DarwinNotificationDetails(
          presentAlert: true,
          presentBadge: true,
          presentSound: true,
        ),
      ),
    );
  }

  /// Get FCM token and register it with the backend.
  Future<void> registerToken({String? email}) async {
    try {
      // iOS requires APNs token before FCM token is available
      if (defaultTargetPlatform == TargetPlatform.iOS) {
        String? apns;
        for (var i = 0; i < 5; i++) {
          apns = await FirebaseMessaging.instance.getAPNSToken();
          if (apns != null) break;
          if (i < 4) await Future.delayed(Duration(seconds: 1 << i)); // 1s, 2s, 4s, 8s
        }
        if (apns == null) {
          debugPrint('APNs token not ready after retries');
          return;
        }
      }

      final token = await FirebaseMessaging.instance.getToken();
      if (token == null) return;

      final dio = _ref.read(dioClientProvider);
      await dio.patch(
        ApiConstants.deviceToken,
        data: {'token': token, 'email': ?email},
      );
      debugPrint('FCM token registered successfully: $token');
    } catch (e) {
      debugPrint('FCM registerToken failed: $e');
    }
  }

  /// Remove the FCM token from the backend on logout.
  /// Silently fails — worst case: stale token stays until next login (overwritten via PATCH).
  Future<void> unregisterToken() async {
    try {
      final dio = _ref.read(dioClientProvider);
      await dio.delete(ApiConstants.deviceToken);
    } catch (e) {
      debugPrint('FCM unregisterToken failed: $e');
    }
  }
}
