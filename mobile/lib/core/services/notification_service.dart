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

@riverpod
NotificationService notificationService(Ref ref) => NotificationService(ref);

class NotificationService {
  NotificationService(this._ref) {
    // Listen for token refreshes
    FirebaseMessaging.instance.onTokenRefresh.listen((token) {
      registerToken();
    });
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

    // Init flutter_local_notifications
    const initSettings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(),
    );
    await _localNotifications.initialize(initSettings);

    // Show local notification when app is in foreground
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
        iOS: DarwinNotificationDetails(),
      ),
    );
  }

  /// Get FCM token and register it with the backend.
  Future<void> registerToken({String? email}) async {
    try {
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
