import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../constants/api_constants.dart';
import '../network/dio_client.dart';
import 'notification_routing.dart';

part 'notification_service.g.dart';

const _channelId = 'high_importance_channel';
const _channelName = 'High Importance Notifications';

final _localNotifications = FlutterLocalNotificationsPlugin();

@pragma('vm:entry-point')
Future<void> firebaseBackgroundHandler(RemoteMessage message) async {
  debugPrint('FCM background message received');
}

@Riverpod(keepAlive: true)
NotificationService notificationService(Ref ref) => NotificationService(ref);

class NotificationService {
  NotificationService(this._ref) {
    if (enabled) {
      FirebaseMessaging.instance.onTokenRefresh.listen((_) => registerToken());
    }
  }

  static bool enabled = false;

  final Ref _ref;

  static Future<void> initialize() async {
    await FirebaseMessaging.instance.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

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

    const initSettings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      ),
    );
    await _localNotifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: _handleLocalNotificationTap,
    );

    await FirebaseMessaging.instance
        .setForegroundNotificationPresentationOptions(
          alert: true,
          badge: true,
          sound: true,
        );

    FirebaseMessaging.onMessage.listen(_showForegroundNotification);

    FirebaseMessaging.onMessageOpenedApp.listen(_handleNotificationClick);

    FirebaseMessaging.instance.getInitialMessage().then((message) {
      if (message != null) {
        _handleNotificationClick(message);
      }
    });
  }

  static final pendingTarget = ValueNotifier<NotificationTarget?>(null);

  static void _handleNotificationClick(RemoteMessage message) {
    pendingTarget.value = notificationTargetFromPush(message.data);
  }

  static void _handleLocalNotificationTap(NotificationResponse response) {
    final payload = response.payload;
    if (payload == null || payload.isEmpty) return;
    try {
      final data = jsonDecode(payload);
      if (data is Map<String, dynamic>) {
        pendingTarget.value = notificationTargetFromPush(data);
      }
    } catch (_) {}
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
      payload: jsonEncode(message.data),
    );
  }

  Future<void> registerToken({String? email}) async {
    if (!enabled) return;
    try {
      if (defaultTargetPlatform == TargetPlatform.iOS) {
        String? apns;
        for (var i = 0; i < 5; i++) {
          apns = await FirebaseMessaging.instance.getAPNSToken();
          if (apns != null) break;
          if (i < 4) await Future.delayed(Duration(seconds: 1 << i));
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
    } catch (e) {
      debugPrint('FCM registerToken failed: $e');
    }
  }

  Future<void> unregisterToken() async {
    try {
      final dio = _ref.read(dioClientProvider);
      await dio.delete(ApiConstants.deviceToken);
    } catch (e) {
      debugPrint('FCM unregisterToken failed: $e');
    }
  }
}
